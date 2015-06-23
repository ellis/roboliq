package roboliq.input

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf
import grizzled.slf4j.Logger
import roboliq.ai.strips
import roboliq.utils.JsonUtils
import roboliq.core.ResultC
import spray.json.JsValue
import spray.json.JsString
import spray.json.JsBoolean
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsNull
import spray.json.JsArray

object JsConverter {
	private val logger = Logger[this.type]
	
	def toString(jsval: JsValue): ResultC[String] = {
		ResultC.context("toString") {
			jsval match {
				case JsString(s) => ResultC.unit(s)
				case _ => ResultC.unit(jsval.toString)
			}
		}
	}
	
	def toInt(jsval: JsValue): ResultC[Int] = {
		toBigDecimal(jsval).map(_.toInt)
	}
	
	def toInteger(jsval: JsValue): ResultC[Integer] = {
		toBigDecimal(jsval).map(_.toInt)
	}
	
	def toFloat(jsval: JsValue): ResultC[Float] = {
		toBigDecimal(jsval).map(_.toFloat)
	}
	
	def toDouble(jsval: JsValue): ResultC[Double] = {
		toBigDecimal(jsval).map(_.toDouble)
	}
	
	def toBigDecimal(jsval: JsValue): ResultC[BigDecimal] = {
		ResultC.context("toBigDecimal") {
			jsval match {
				case JsBoolean(b) => ResultC.unit(if (b) 1 else 0)
				case JsNumber(n) => ResultC.unit(n)
				case _ => ResultC.error("expected JsNumber")
			}
		}
	}
	
	/*def toBigDecimal(scope: Map[String, JsValue], name: String): ResultC[BigDecimal] = {
		scope.get(name) match {
			case None => ResultC.error(s"variable `$name` missing from scope")
			case Some(jsval) => toBigDecimal(jsval)
		}
	}*/
	
	def toBoolean(jsval: JsValue): ResultC[java.lang.Boolean] = {
		ResultC.context("toBoolean") {
			jsval match {
				case JsBoolean(b) => ResultC.unit(b)
				case JsNumber(n) => ResultC.unit(n != 0)
				case JsString(s) => ResultC.unit(s == "true")
				case _ => ResultC.error("expected JsBoolean")
			}
		}
	}
	
	def toEnum[A <: Enumeration#Value : TypeTag](jsval: JsValue): ResultC[A] = {
		val typ = ru.typeTag[A].tpe
		toEnum(jsval, typ).map(_.asInstanceOf[A])
	}
	
	private def toEnum(jsval: JsValue, typ: ru.Type): ResultC[Any] = {
		try {
			//val mirror = clazz_?.map(clazz => ru.runtimeMirror(clazz.getClassLoader)).getOrElse(scala.reflect.runtime.currentMirror)
			val mirror = ru.runtimeMirror(getClass.getClassLoader)
			// Get enclosing enumeration (e.g. MyStatus.Value => MyStatus)
			val enumType = typ.find(_ <:< typeOf[Enumeration]).get
			val enumModule = enumType.termSymbol.asModule
			val enumMirror = mirror.reflectModule(enumModule)
			val enum = enumMirror.instance.asInstanceOf[Enumeration]
			val value_l = enum.values
			jsval match {
				case JsString(s) =>
					ResultC.from(value_l.find(_.toString == s), s"Value '$s' not valid for `${enumModule.name}`.  Expected one of ${value_l.mkString(", ")}.")
				case _ => ResultC.error("expected JsString")
			}
		} catch {
			case e: Throwable => ResultC.error(s"type: $typ, jsval: $jsval, error: ${e.getMessage}")
		}
	}

	
	def toStripsLiteral(
		jsval: JsValue
	): ResultC[strips.Literal] = {
		for {
			s <- JsConverter.toString(jsval)
			lit <- s.split(" ").toList.filterNot(_.isEmpty) match {
				case Nil =>
					ResultC.error("expected a non-empty string for logical literal")
				case l =>
					val name0 = l.head
					val pos = !name0.startsWith("~")
					val name = if (pos) name0 else name0.substring(1)
					if (name.isEmpty) {
						ResultC.error(s"expected a non-empty name for logical literal in: $s")
					}
					else {
						ResultC.unit(strips.Literal(strips.Atom(name, l.tail), pos))
					}
			}
		} yield lit
	}
	
	def toStripsLiterals(
		jsval: JsValue
	): ResultC[strips.Literals] = {
		for {
			l <- fromJs[List[strips.Literal]](jsval)
		} yield strips.Literals(roboliq.ai.plan.Unique(l : _*))
	}
	
	def fromJs[A: TypeTag](
		jsval: JsValue
	): ResultC[A] = {
		//println(s"fromJs($jsval)")
		val typ = ru.typeTag[A].tpe
		for {
			o <- conv(jsval, typ)
			//_ <- ResultC.assert(o.isInstanceOf[A], s"INTERNAL: mis-converted JSON: `$jsval` to `$o`")
		} yield o.asInstanceOf[A]
	}

	def fromJs[A: TypeTag](map: JsObject, field: String): ResultC[A] =
		fromJs[A](map.fields, field)

	def fromJs[A: TypeTag](map: Map[String, JsValue], field: String): ResultC[A] = {
		ResultC.context(field) {
			map.get(field) match {
				case Some(jsval) => fromJs[A](jsval)
				case None =>
					ResultC.orElse(
						fromJs[A](JsNull),
						ResultC.error("value required")
					)
			}
		}
	}

	def fromJs[A: TypeTag](map: JsObject, field_l: Seq[String]): ResultC[A] = {
		if (field_l.isEmpty) {
			fromJs[A](map)
		}
		else {
			map.fields.get(field_l.head) match {
				case None => ResultC.error("missing property ${field_l.head}")
				case Some(jsobj2: JsObject) => fromJs[A](jsobj2, field_l.tail)
				case Some(jsval2) =>
					val rest = field_l.tail
					if (rest.isEmpty) {
						fromJs[A](jsval2)
					}
					else {
						ResultC.error(s"value does not contain field `${field_l.head}`: $jsval2")
					}
			}
		}
	}

	private def conv(
		jsval: JsValue,
		typ: Type,
		path_? : Option[String] = None
	): ResultC[Any] = {
		logger.debug(s"conv($jsval, $typ)")
		import scala.reflect.runtime.universe._

		val mirror = runtimeMirror(this.getClass.getClassLoader)

		//val path = path_r.reverse.mkString(".")
		//val prefix = if (path_r.isEmpty) "" else path + ": "
		//logger.trace(s"conv(${path}, $jsval, $typ, eb)")

		val ctx = {
			val ret: ResultC[Any] = {
				if (typ =:= typeOf[String]) JsConverter.toString(jsval)
				else if (typ =:= typeOf[Int]) toInt(jsval)
				else if (typ =:= typeOf[Integer]) toInteger(jsval)
				else if (typ =:= typeOf[Float]) toFloat(jsval)
				else if (typ =:= typeOf[Double]) toDouble(jsval)
				else if (typ =:= typeOf[BigDecimal]) toBigDecimal(jsval)
				else if (typ =:= typeOf[Boolean]) toBoolean(jsval)//.map(_.asInstanceOf[Boolean])
				else if (typ =:= typeOf[java.lang.Boolean]) toBoolean(jsval)
				else if (typ <:< typeOf[Enumeration#Value]) toEnum(jsval, typ)
				/*
				else if (typ =:= typeOf[AmountSpec]) toAmountSpec(jsval)
				else if (typ =:= typeOf[LiquidSource]) toLiquidSource(jsval, eb, state_?)
				else if (typ =:= typeOf[LiquidVolume]) toVolume(jsval)
				else if (typ =:= typeOf[PipetteAmount]) toPipetteAmount(jsval)
				else if (typ =:= typeOf[PipetteDestination]) toPipetteDestination(jsval, eb, state_?)
				else if (typ =:= typeOf[PipetteDestinations]) toPipetteDestinations(jsval, eb, state_?)
				else if (typ =:= typeOf[PipetteSources]) toPipetteSources(jsval, eb, state_?)
				*/
				// Logic
				else if (typ =:= typeOf[strips.Literal]) toStripsLiteral(jsval)
				else if (typ =:= typeOf[strips.Literals]) toStripsLiterals(jsval)
				/*
				// Lookups
				else if (typ =:= typeOf[Agent]) toEntityByRef[Agent](jsval, eb)
				else if (typ =:= typeOf[Labware]) toEntityByRef[Labware](jsval, eb)
				else if (typ =:= typeOf[Pipetter]) toEntityByRef[Pipetter](jsval, eb)
				else if (typ =:= typeOf[Shaker]) toEntityByRef[Shaker](jsval, eb)
				else if (typ =:= typeOf[TipModel]) toEntityByRef[TipModel](jsval, eb)
				//else if (typ <:< typeOf[Substance]) toSubstance(jsval)
				*/
				else if (typ =:= typeOf[JsValue]) {
					ResultC.unit(jsval)
				}
				else if (typ <:< typeOf[JsValue]) {
					if (mirror.runtimeClass(typ).isInstance(jsval)) {
						ResultC.unit(jsval)
					}
					else if (jsval == JsNull) {
						if (typ =:= typeOf[JsObject]) ResultC.unit(JsObject())
						else if (typ =:= typeOf[JsArray]) ResultC.unit(JsArray())
						else ResultC.error(s"Could not convert JsNull to TYPE=$typ")
					}
					else {
						ResultC.error(s"Could not convert to TYPE=$typ from $jsval")
					}
				}
				else if (typ <:< typeOf[Option[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					if (jsval == JsNull) ResultC.unit(None)
					else conv(jsval, typ2).map(o => Option(o))
				}
				else if (typ <:< typeOf[Set[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					jsval match {
						case JsNull =>
							ResultC.unit(Set())
						case m: JsObject =>
							convSet(m, typ2)
						case _ =>
							convList(jsval, typ2).map(l => Set(l : _*))
					}
				}
				else if (typ <:< typeOf[Map[_, _]]) {
					jsval match {
						case m: JsObject =>
							//println("fields: " + fields)
							val typKey = typ.asInstanceOf[ru.TypeRefApi].args(0)
							val typVal = typ.asInstanceOf[ru.TypeRefApi].args(1)
							val name_l = m.fields.keys.toList
							val nameToType_l = name_l.map(_ -> typVal)
							for {
								res <- convMap(m, typKey, nameToType_l)
							} yield res
						case JsNull => ResultC.unit(Map())
						case _ =>
							ResultC.error("expected a JsObject")
					}
				}
				else if (typ <:< typeOf[List[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					convList(jsval, typ2)
				}
				else {
					//println("typ: "+typ)
					val ctor = typ.member(termNames.CONSTRUCTOR).asMethod
					val p0_l = ctor.paramLists(0)
					val nameToType_l = p0_l.map(p => p.name.decodedName.toString.replace("_?", "") -> p.typeSignature)
					for {
						nameToObj_m <- jsval match {
							case m: JsObject =>
								convMapString(m, nameToType_l)
							case JsNull =>
								convMapString(JsObject(), nameToType_l)
							case _ =>
								ResultC.error(s"unhandled type or value. type=${typ}, value=${jsval}")
						}
					} yield {
						val arg_l = nameToType_l.map(pair => nameToObj_m(pair._1))
						val c = typ.typeSymbol.asClass
						//println("arg_l: "+arg_l)
						val mm = mirror.reflectClass(c).reflectConstructor(ctor)
						//logger.debug("arg_l: "+arg_l)
						val obj = mm(arg_l : _*)
						obj
					}
				}
			}
			ret
		}
		path_? match {
			case None => ctx
			case Some(path) => ResultC.context(path)(ctx)
		}
	}

		
	private def convList(
		jsval: JsValue,
		typ2: Type
	): ResultC[List[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		jsval match {
			case JsArray(v) =>
				ResultC.mapAll(v.zipWithIndex) { case (rjsval2, i0) =>
					val i = i0 + 1
					conv(rjsval2, typ2, Some(s"[$i]"))
				}
			case JsNull =>
				ResultC.unit(Nil)
			case _ =>
				ResultC.or(
					conv(jsval, typ2).map(List(_)),
					ResultC.error(s"expected an array of ${typ2.typeSymbol.name.toString}.  Instead found: ${jsval}")
				)
		}
	}
	
	private def convSet(
		m: JsObject,
		typ2: Type
	): ResultC[Set[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		// Try to convert each element of the array
		ResultC.mapAll(m.fields.toList) ({ case (id, jsval) =>
			conv(jsval, typ2, Some(id))
		}).map(l => Set(l : _*))
	}

	private def convListToObject(
		rjsval_l: List[JsValue],
		nameToType_l: List[(String, ru.Type)]
	): ResultC[Map[String, _]] = {
		ResultC.error("convListToObject: not yet implemented")
	}

	def convMap(
		m: JsObject,
		typKey: Type,
		nameToType_l: List[(String, ru.Type)]
	): ResultC[Map[_, _]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)
		
		/*println("convMap: ")
		println(path_r)
		println(jsobj)
		println(typKey)
		println(nameToType_l)*/

		// TODO: Handle keys if they need to be looked up -- this just uses strings
		val key_l = nameToType_l.map(_._1)
		val map = m.fields
		
		// Try to convert each element of the object
		for {
			val_l <- ResultC.map(nameToType_l) { case (name, typ2) =>
				map.get(name) match {
					case Some(jsval2) => conv(jsval2, typ2, Some(name))
					// Field is missing, so try using JsNull
					case None => conv(JsNull, typ2, Some(name))
				}
			}
		} yield {
			(key_l zip val_l).toMap
		}
	}

	def convMapString(
		m: JsObject,
		nameToType_l: List[(String, ru.Type)]
	): ResultC[Map[String, _]] = {
		for {
			map <- convMap(m, typeOf[String], nameToType_l)
		} yield map.asInstanceOf[Map[String, _]]
	}
	
	def mergeObjects[A : TypeTag](o1: A, o2: A): ResultC[A] = {
		val typ = ru.typeTag[A].tpe
		for {
			o <- mergeObjects(o1, o2, typ)
			//_ <- ResultC.assert(o.isInstanceOf[A], s"INTERNAL: mis-converted JSON: `$jsval` to `$o`")
		} yield o.asInstanceOf[A]
	}

	private def mergeObjects(o1: Any, o2: Any, typ: Type): ResultC[Any] = {
		import scala.reflect.runtime.universe._

		val mirror = runtimeMirror(this.getClass.getClassLoader)
		
		if (typ <:< typeOf[JsValue]) {
			val v1 = o1.asInstanceOf[JsValue]
			val v2 = o2.asInstanceOf[JsValue]
			JsonUtils.merge(v1, v2)
		}
		else if (typ <:< typeOf[Option[_]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
			val v1 = o1.asInstanceOf[Option[_]]
			val v2 = o2.asInstanceOf[Option[_]]
			(v1, v2) match {
				case (None, None) => ResultC.unit(None)
				case (_, Some(null)) => ResultC.unit(None)
				case (None, _) => ResultC.unit(o2)
				case (Some(null), _) => ResultC.unit(o2)
				case (Some(_), None) => ResultC.unit(o1)
				case (Some(x1), Some(x2)) => mergeObjects(x1, x2, typ2).map(Option.apply)
			}
		}
		else if (typ <:< typeOf[List[_]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
			val v1 = o1.asInstanceOf[List[_]]
			val v2 = o2.asInstanceOf[List[_]]
			val v3 = v1 ++ v2
			ResultC.unit(v3)
		}
		else if (typ <:< typeOf[Set[_]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
			val v1 = o1.asInstanceOf[Set[_]]
			val v2 = o2.asInstanceOf[Set[_]]
			val v3 = v1 ++ v2
			ResultC.unit(v3)
		}
		else if (typ <:< typeOf[Map[String, _]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args(1)
			val m1 = o1.asInstanceOf[Map[String, _]]
			val m2 = o2.asInstanceOf[Map[String, _]]
			val key_l = m1.keySet ++ m2.keySet
			for {
				merged_l <- ResultC.map(key_l.toSeq) { key =>
					(m1.get(key), m2.get(key)) match {
						case (None, None) => ???
						case (Some(o1), None) => ResultC.unit(key -> o1)
						case (None, Some(o2)) => ResultC.unit(key -> o2)
						case (Some(o1), Some(o2)) => mergeObjects(o1, o2, typ2).map(key -> _)
					}
				}
			} yield merged_l.toMap
		}
		else {
			if (o2 == null)
				ResultC.unit(null)
			else if (o1 == null)
				ResultC.unit(o2)
			else {
				for {
					rjsval1 <- RjsValue.toJson(o1, typ)
					rjsval2 <- RjsValue.toJson(o2, typ)
					rjsval3 <- JsonUtils.merge(rjsval1, rjsval2)
					o <- conv(rjsval3, typ)
				} yield o
			}
		}
	}
}