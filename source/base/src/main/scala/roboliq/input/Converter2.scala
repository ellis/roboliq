package roboliq.input

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf
import grizzled.slf4j.Logger
import spray.json.JsValue
import spray.json.JsString
import spray.json.JsObject
import roboliq.core.RsError
import spray.json.JsNumber
import spray.json.JsBoolean
import spray.json.JsNull
import spray.json.JsArray

object Converter2 {
	private val logger = Logger[this.type]
	
	def toString(jsval: JsValue): ResultE[String] = {
		ResultE.context("toString") {
			jsval match {
				case JsString(text) => ResultE.unit(text)
				case JsObject(m) =>
					for {
						_ <- ResultE.assert(m.get("TYPE") == "string", s"expected JsString or TYPE=string")
						jsval2 <- ResultE.from(m.get("VALUE"), s"expected VALUE for string")
						x <- toString(jsval2)
					} yield x
				case _ => ResultE.error("expected JsString")
			}
		}
	}
	
	def toInt(jsval: JsValue): ResultE[Int] = {
		toBigDecimal(jsval).map(_.toInt)
	}
	
	def toInteger(jsval: JsValue): ResultE[Integer] = {
		toBigDecimal(jsval).map(_.toInt)
	}
	
	def toDouble(jsval: JsValue): ResultE[Double] = {
		toBigDecimal(jsval).map(_.toDouble)
	}
	
	def toBigDecimal(jsval: JsValue): ResultE[BigDecimal] = {
		ResultE.context("toBigDecimal") {
			jsval match {
				case JsObject(m) =>
					(m.get("TYPE"), m.get("VALUE")) match {
						case (Some(JsString("number")), Some(JsNumber(n))) =>
							ResultE.unit(n)
						case _ =>
							ResultE.error("Expected TYPE=number and VALUE of type JsNumber: "+jsval)
					}
				case JsNumber(n) => ResultE.unit(n)
				case _ => ResultE.error("expected JsNumber or JsObject")
			}
		}
	}
	
	def toBigDecimal(scope: Map[String, JsValue], name: String): ResultE[BigDecimal] = {
		scope.get(name) match {
			case None => ResultE.error(s"variable `$name` missing from scope")
			case Some(jsval) => toBigDecimal(jsval)
		}
	}
	
	def toBoolean(jsval: JsValue): ResultE[java.lang.Boolean] = {
		ResultE.context("toBoolean") {
			jsval match {
				case JsObject(m) =>
					for {
						_ <- ResultE.assert(m.get("TYPE") == "boolean", s"expected JsBoolean or TYPE=boolean")
						jsval2 <- ResultE.from(m.get("VALUE"), s"expected VALUE for boolean")
						x <- toBoolean(jsval2)
					} yield x
				case JsBoolean(b) => ResultE.unit(b)
				case _ => ResultE.error("expected JsBoolean")
			}
		}
	}
	
	def toEnum[A <: Enumeration#Value : TypeTag](jsval: JsValue): ResultE[A] = {
		val typ = ru.typeTag[A].tpe
		toEnum(jsval, typ).map(_.asInstanceOf[A])
	}
	
	private def toEnum(jsval: JsValue, typ: ru.Type): ResultE[Any] = {
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
					ResultE.from(value_l.find(_.toString == s), s"Value '$s' not valid for `${enumModule.name}`.  Expected one of ${value_l.mkString(", ")}.")
				case _ => ResultE.error("expected JsString")
			}
		} catch {
			case e: Throwable => ResultE.error(s"type: $typ, jsval: $jsval, error: ${e.getMessage}")
		}
	}

	def makeBuild(item_l: List[(String, JsObject)]): JsObject = {
		val item1_l = item_l.map { case (op, jsobj) => JsObject(Map(op -> jsobj))}
		JsObject(Map("TYPE" -> JsString("build"), "ITEM" -> JsArray(item1_l)))
	}

	def makeCall(name: String, input: Map[String, JsValue]): JsObject = {
		JsObject(Map("TYPE" -> JsString("call"), "NAME" -> JsString(name), "INPUT" -> JsObject(input)))
	}

	def makeImport(name: String, version: String): JsObject = {
		JsObject(Map("TYPE" -> JsString("import"), "NAME" -> JsString(name), "VERSION" -> JsString(version)))
	}

	def makeInclude(filename: String): JsObject = {
		JsObject(Map("TYPE" -> JsString("include"), "FILENAME" -> JsString(filename)))
	}
	
	def makeInstruction(
		name: String,
		input: Map[String, JsValue]
	): JsObject = {
		JsObject(Map("TYPE" -> JsString("instruction"), "NAME" -> JsString(name), "INPUT" -> JsObject(input)))
	}

	def makeLambda(param: List[String], expression: JsValue): JsObject = {
		JsObject(Map("TYPE" -> JsString("lambda"), "EXPRESSION" -> expression))
	}

	def makeLet(var_l: List[(String, JsValue)], expression: JsValue): JsObject = {
		val var1_l = var_l.map { case (name, jsval) =>
			JsObject(Map(name -> jsval))
		}
		JsObject(Map("TYPE" -> JsString("let"), "VAR" -> JsArray(var1_l), "EXPRESSION" -> expression))
	}

	def makeList(l: List[JsValue]): JsObject = {
		JsObject(Map("TYPE" -> JsString("list"), "VALUE" -> JsArray(l)))
	}
	
	def makeMap(map: Map[String, JsValue]): JsObject = {
		JsObject(Map("TYPE" -> JsString("map"), "VALUE" -> JsObject(map)))
	}
	
	def makeNumber(n: BigDecimal): JsObject = {
		JsObject(Map("TYPE" -> JsString("number"), "VALUE" -> JsNumber(n)))
	}
	
	def makeString(s: String): JsObject = {
		JsObject(Map("TYPE" -> JsString("string"), "VALUE" -> JsString(s)))
	}
	
	def makeStringf(s: String): JsObject = {
		JsObject(Map("TYPE" -> JsString("stringf"), "VALUE" -> JsString(s)))
	}
	
	def makeSubst(name: String): JsObject = {
		JsObject(Map("TYPE" -> JsString("subst"), "NAME" -> JsString(name)))
	}
	
	def fromJson[A: TypeTag](
		jsval: JsValue
	): ResultE[A] = {
		//println(s"fromJson($jsval)")
		val typ = ru.typeTag[A].tpe
		for {
			o <- conv(jsval, typ)
			//_ <- ResultE.assert(o.isInstanceOf[A], s"INTERNAL: mis-converted JSON: `$jsval` to `$o`")
		} yield o.asInstanceOf[A]
	}

	def fromJson[A: TypeTag](
		jsobj: JsObject,
		field: String
	): ResultE[A] = {
		ResultE.context(field) {
			jsobj.fields.get(field) match {
				case Some(jsval) => Converter2.fromJson[A](jsval)
				case None =>
					ResultE.orElse(
						Converter2.fromJson[A](JsNull),
						ResultE.error("value required")
					)
			}
		}
	}

	private def conv(
		jsval: JsValue,
		typ: Type,
		path_? : Option[String] = None
	): ResultE[Any] = {
		println(s"conv($jsval, $typ)")
		import scala.reflect.runtime.universe._

		val mirror = runtimeMirror(this.getClass.getClassLoader)

		//val path = path_r.reverse.mkString(".")
		//val prefix = if (path_r.isEmpty) "" else path + ": "
		//logger.trace(s"conv(${path}, $jsval, $typ, eb)")

		val ctx = {
			val ret: ResultE[Any] = {
				if (typ <:< typeOf[JsValue]) ResultE.unit(jsval)
				else if (typ =:= typeOf[String]) Converter2.toString(jsval)
				else if (typ =:= typeOf[Int]) toInt(jsval)
				else if (typ =:= typeOf[Integer]) toInteger(jsval)
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
				// Logic
				else if (typ =:= typeOf[strips.Literal]) tostripsLiteral(jsval)
				// Lookups
				else if (typ =:= typeOf[Agent]) toEntityByRef[Agent](jsval, eb)
				else if (typ =:= typeOf[Labware]) toEntityByRef[Labware](jsval, eb)
				else if (typ =:= typeOf[Pipetter]) toEntityByRef[Pipetter](jsval, eb)
				else if (typ =:= typeOf[Shaker]) toEntityByRef[Shaker](jsval, eb)
				else if (typ =:= typeOf[TipModel]) toEntityByRef[TipModel](jsval, eb)
				//else if (typ <:< typeOf[Substance]) toSubstance(jsval)
				*/
				//else if (typ <:< typeOf[RjsValue]) {
				//	RjsValue.fromJson(jsval)
				//}
				else if (typ <:< typeOf[Option[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					if (jsval == JsNull) ResultE.unit(None)
					else conv(jsval, typ2).map(o => Option(o))
				}
				else if (typ <:< typeOf[List[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					convList(jsval, typ2)
				}
				else if (typ <:< typeOf[Set[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					jsval match {
						case jsobj @ JsObject(fields) =>
							convSet(jsobj, typ2)
						case _ =>
							convList(jsval, typ2).map(l => Set(l : _*))
					}
				}
				else if (typ <:< typeOf[Map[_, _]]) {
					jsval match {
						case jsobj @ JsObject(fields) =>
							//println("fields: " + fields)
							val typKey = typ.asInstanceOf[ru.TypeRefApi].args(0)
							val typVal = typ.asInstanceOf[ru.TypeRefApi].args(1)
							val name_l = fields.toList.map(_._1)
							val nameToType_l = name_l.map(_ -> typVal)
							for {
								res <- convMap(jsobj, typKey, nameToType_l)
							} yield res
						case JsNull => ResultE.unit(Map())
						case _ =>
							ResultE.error("expected a JsObject")
					}
				}
				else {
					//println("typ: "+typ)
					val ctor = typ.member(termNames.CONSTRUCTOR).asMethod
					val p0_l = ctor.paramLists(0)
					val nameToType_l = p0_l.map(p => p.name.decodedName.toString.replace("_?", "") -> p.typeSignature)
					for {
						nameToObj_m <- jsval match {
							case jsobj: JsObject =>
								convMapString(jsobj, nameToType_l)
							case _ =>
								ResultE.error(s"unhandled type or value. type=${typ}, value=${jsval}")
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
			case Some(path) => ResultE.context(path)(ctx)
		}
	}

		
	private def convList(
		jsval: JsValue,
		typ2: Type
	): ResultE[List[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		jsval match {
			case JsObject(map) =>
				(map.get("TYPE"), map.get("VALUE")) match {
					case (Some(JsString("list")), Some(JsArray(v))) =>
						ResultE.mapAll(v.zipWithIndex) { case (jsval2, i0) =>
							val i = i0 + 1
							conv(jsval2, typ2, Some(s"[$i]"))
						}
					case _ =>
						ResultE.error(s"expected an array of ${typ2.typeSymbol.name.toString}.  Instead found: ${jsval}")
				}
			case JsArray(v) =>
				ResultE.mapAll(v.zipWithIndex) { case (jsval2, i0) =>
					val i = i0 + 1
					conv(jsval2, typ2, Some(s"[$i]"))
				}
			case JsNull =>
				ResultE.unit(Nil)
			case _ =>
				ResultE.or(
					conv(jsval, typ2).map(List(_)),
					ResultE.error(s"expected an array of ${typ2.typeSymbol.name.toString}.  Instead found: ${jsval}")
				)
		}
	}
	
	private def convSet(
		jsobj: JsObject,
		typ2: Type
	): ResultE[Set[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		// Try to convert each element of the array
		ResultE.mapAll(jsobj.fields.toList) ({ case (id, jsval) =>
			conv(jsval, typ2, Some(id))
		}).map(l => Set(l : _*))
	}

	private def convListToObject(
		jsval_l: List[JsValue],
		nameToType_l: List[(String, ru.Type)]
	): ResultE[Map[String, _]] = {
		ResultE.error("convListToObject: not yet implemented")
	}

	def convMap(
		jsobj: JsObject,
		typKey: Type,
		nameToType_l: List[(String, ru.Type)]
	): ResultE[Map[_, _]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)
		
		/*println("convMap: ")
		println(path_r)
		println(jsobj)
		println(typKey)
		println(nameToType_l)*/

		// TODO: Handle keys if they need to be looked up -- this just uses strings
		val key_l = nameToType_l.map(_._1)
		
		// Try to convert each element of the object
		for {
			val_l <- ResultE.map(nameToType_l) { case (name, typ2) =>
				jsobj.fields.get(name) match {
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
		jsobj: JsObject,
		nameToType_l: List[(String, ru.Type)]
	): ResultE[Map[String, _]] = {
		for {
			map <- convMap(jsobj, typeOf[String], nameToType_l)
		} yield map.asInstanceOf[Map[String, _]]
	}
}