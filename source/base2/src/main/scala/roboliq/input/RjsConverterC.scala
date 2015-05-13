package roboliq.input

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf
import grizzled.slf4j.Logger
import roboliq.ai.strips
import roboliq.utils.JsonUtils
import roboliq.core.ResultC

// REFACTOR: Regarding RjsConverter/RjsConverterC, I should probably get rid of RjsConverter and perform evaluation completely separately
// RjsConverter is like RjsConverterC, but it also does evaluation.
object RjsConverterC {
	private val logger = Logger[this.type]
	
	def toString(rjsval: RjsValue): ResultC[String] = {
		
		ResultC.context("toString") {
			rjsval match {
				case x: RjsString => ResultC.unit(x.s)
				case x: RjsText => ResultC.unit("\""+x.text+"\"")
				case x: RjsFormat => ResultC.unit("f\""+x.format+"\"")
				case x: RjsSubst => ResultC.unit("$"+x.name)
				case _ =>
					ResultC.error(s"cannot convert to String: $rjsval")
			}
		}
	}
	
	def toInt(rjsval: RjsValue): ResultC[Int] = {
		toBigDecimal(rjsval).map(_.toInt)
	}
	
	def toInteger(rjsval: RjsValue): ResultC[Integer] = {
		toBigDecimal(rjsval).map(_.toInt)
	}
	
	def toFloat(rjsval: RjsValue): ResultC[Float] = {
		toBigDecimal(rjsval).map(_.toFloat)
	}
	
	def toDouble(rjsval: RjsValue): ResultC[Double] = {
		toBigDecimal(rjsval).map(_.toDouble)
	}
	
	def toBigDecimal(rjsval: RjsValue): ResultC[BigDecimal] = {
		ResultC.context("toBigDecimal") {
			rjsval match {
				case RjsNumber(n, None) => ResultC.unit(n)
				case RjsNumber(_, _) => ResultC.error("expected RjsNumber without units: "+rjsval.toText)
				case _ => ResultC.error("expected RjsNumber")
			}
		}
	}
	
	/*def toBigDecimal(scope: Map[String, RjsValue], name: String): ResultC[BigDecimal] = {
		scope.get(name) match {
			case None => ResultC.error(s"variable `$name` missing from scope")
			case Some(rjsval) => toBigDecimal(rjsval)
		}
	}*/
	
	def toBoolean(rjsval: RjsValue): ResultC[java.lang.Boolean] = {
		ResultC.context("toBoolean") {
			rjsval match {
				case RjsBoolean(b) => ResultC.unit(b)
				case RjsNumber(n, _) => ResultC.unit(n != 0)
				case RjsString(s) => ResultC.unit(s == "true")
				case _ => ResultC.error("expected RjsBoolean")
			}
		}
	}
	
	def toEnum[A <: Enumeration#Value : TypeTag](rjsval: RjsValue): ResultC[A] = {
		val typ = ru.typeTag[A].tpe
		toEnum(rjsval, typ).map(_.asInstanceOf[A])
	}
	
	private def toEnum(rjsval: RjsValue, typ: ru.Type): ResultC[Any] = {
		try {
			//val mirror = clazz_?.map(clazz => ru.runtimeMirror(clazz.getClassLoader)).getOrElse(scala.reflect.runtime.currentMirror)
			val mirror = ru.runtimeMirror(getClass.getClassLoader)
			// Get enclosing enumeration (e.g. MyStatus.Value => MyStatus)
			val enumType = typ.find(_ <:< typeOf[Enumeration]).get
			val enumModule = enumType.termSymbol.asModule
			val enumMirror = mirror.reflectModule(enumModule)
			val enum = enumMirror.instance.asInstanceOf[Enumeration]
			val value_l = enum.values
			rjsval match {
				case RjsString(s) =>
					ResultC.from(value_l.find(_.toString == s), s"Value '$s' not valid for `${enumModule.name}`.  Expected one of ${value_l.mkString(", ")}.")
				case _ => ResultC.error("expected RjsString")
			}
		} catch {
			case e: Throwable => ResultC.error(s"type: $typ, rjsval: $rjsval, error: ${e.getMessage}")
		}
	}

	
	def toStripsLiteral(
		rjsval: RjsValue
	): ResultC[strips.Literal] = {
		for {
			s <- RjsConverterC.toString(rjsval)
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
		rjsval: RjsValue
	): ResultC[strips.Literals] = {
		for {
			l <- fromRjs[List[strips.Literal]](rjsval)
		} yield strips.Literals(roboliq.ai.plan.Unique(l : _*))
	}
	
	def fromRjs[A: TypeTag](
		rjsval: RjsValue
	): ResultC[A] = {
		//println(s"fromRjson($rjsval)")
		val typ = ru.typeTag[A].tpe
		for {
			o <- conv(rjsval, typ)
			//_ <- ResultC.assert(o.isInstanceOf[A], s"INTERNAL: mis-converted JSON: `$rjsval` to `$o`")
		} yield o.asInstanceOf[A]
	}

	def fromRjs[A: TypeTag](map: RjsAbstractMap, field: String): ResultC[A] =
		fromRjs[A](map.getValueMap, field)

	def fromRjs[A: TypeTag](map: Map[String, RjsValue], field: String): ResultC[A] = {
		ResultC.context(field) {
			map.get(field) match {
				case Some(rjsval) => fromRjs[A](rjsval)
				case None =>
					ResultC.orElse(
						fromRjs[A](RjsNull),
						ResultC.error("value required")
					)
			}
		}
	}

	private def conv(
		rjsval: RjsValue,
		typ: Type,
		path_? : Option[String] = None
	): ResultC[Any] = {
		println(s"conv($rjsval, $typ)")
		import scala.reflect.runtime.universe._

		val mirror = runtimeMirror(this.getClass.getClassLoader)

		//val path = path_r.reverse.mkString(".")
		//val prefix = if (path_r.isEmpty) "" else path + ": "
		//logger.trace(s"conv(${path}, $rjsval, $typ, eb)")

		val ctx = {
			val ret: ResultC[Any] = {
				if (typ =:= typeOf[String]) RjsConverterC.toString(rjsval)
				else if (typ =:= typeOf[Int]) toInt(rjsval)
				else if (typ =:= typeOf[Integer]) toInteger(rjsval)
				else if (typ =:= typeOf[Float]) toFloat(rjsval)
				else if (typ =:= typeOf[Double]) toDouble(rjsval)
				else if (typ =:= typeOf[BigDecimal]) toBigDecimal(rjsval)
				else if (typ =:= typeOf[Boolean]) toBoolean(rjsval)//.map(_.asInstanceOf[Boolean])
				else if (typ =:= typeOf[java.lang.Boolean]) toBoolean(rjsval)
				else if (typ <:< typeOf[Enumeration#Value]) toEnum(rjsval, typ)
				/*
				else if (typ =:= typeOf[AmountSpec]) toAmountSpec(rjsval)
				else if (typ =:= typeOf[LiquidSource]) toLiquidSource(rjsval, eb, state_?)
				else if (typ =:= typeOf[LiquidVolume]) toVolume(rjsval)
				else if (typ =:= typeOf[PipetteAmount]) toPipetteAmount(rjsval)
				else if (typ =:= typeOf[PipetteDestination]) toPipetteDestination(rjsval, eb, state_?)
				else if (typ =:= typeOf[PipetteDestinations]) toPipetteDestinations(rjsval, eb, state_?)
				else if (typ =:= typeOf[PipetteSources]) toPipetteSources(rjsval, eb, state_?)
				*/
				// Logic
				else if (typ =:= typeOf[strips.Literal]) toStripsLiteral(rjsval)
				else if (typ =:= typeOf[strips.Literals]) toStripsLiterals(rjsval)
				/*
				// Lookups
				else if (typ =:= typeOf[Agent]) toEntityByRef[Agent](rjsval, eb)
				else if (typ =:= typeOf[Labware]) toEntityByRef[Labware](rjsval, eb)
				else if (typ =:= typeOf[Pipetter]) toEntityByRef[Pipetter](rjsval, eb)
				else if (typ =:= typeOf[Shaker]) toEntityByRef[Shaker](rjsval, eb)
				else if (typ =:= typeOf[TipModel]) toEntityByRef[TipModel](rjsval, eb)
				//else if (typ <:< typeOf[Substance]) toSubstance(rjsval)
				*/
				else if (typ =:= typeOf[RjsValue]) {
					ResultC.unit(rjsval)
				}
				else if (typ <:< typeOf[RjsValue]) {
					if (mirror.runtimeClass(typ).isInstance(rjsval)) {
						ResultC.unit(rjsval)
					}
					else if (rjsval == RjsNull) {
						if (typ =:= typeOf[RjsBasicMap]) ResultC.unit(RjsBasicMap())
						else if (typ =:= typeOf[RjsMap]) ResultC.unit(RjsMap())
						else if (typ =:= typeOf[RjsList]) ResultC.unit(RjsList())
						else ResultC.error(s"Could not convert RjsNull to TYPE=$typ")
					}
					else if (rjsval.isInstanceOf[RjsAbstractMap]) {
						val m = rjsval.asInstanceOf[RjsAbstractMap]
						if (typ =:= typeOf[RjsMap]) {
							ResultC.unit(RjsMap(m.getValueMap))
						}
						else if (typ =:= typeOf[RjsBasicMap]) {
							for {
								map <- ResultC.map(m.getValueMap.toList) { case (name, rjsval) =>
									for {
										basic <- RjsValue.toBasicValue(rjsval)
									} yield name -> basic
								}
							} yield RjsBasicMap(map.toMap)
						}
						else {
							ResultC.error(s"Could not convert to TYPE=$typ from map ${m}")
						}
					}
					else {
						ResultC.error(s"Could not convert to TYPE=$typ from $rjsval")
					}
				}
				else if (typ <:< typeOf[Option[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					if (rjsval == RjsNull) ResultC.unit(None)
					else conv(rjsval, typ2).map(o => Option(o))
				}
				else if (typ <:< typeOf[Set[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					rjsval match {
						case RjsNull =>
							ResultC.unit(Set())
						case m: RjsAbstractMap =>
							convSet(m, typ2)
						case _ =>
							convList(rjsval, typ2).map(l => Set(l : _*))
					}
				}
				else if (typ <:< typeOf[Map[_, _]]) {
					rjsval match {
						case m: RjsAbstractMap =>
							//println("fields: " + fields)
							val typKey = typ.asInstanceOf[ru.TypeRefApi].args(0)
							val typVal = typ.asInstanceOf[ru.TypeRefApi].args(1)
							val name_l = m.getValueMap.keys.toList
							val nameToType_l = name_l.map(_ -> typVal)
							for {
								res <- convMap(m, typKey, nameToType_l)
							} yield res
						case RjsNull => ResultC.unit(Map())
						case _ =>
							ResultC.error("expected a RjsMap")
					}
				}
				else if (typ <:< typeOf[List[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					convList(rjsval, typ2)
				}
				else {
					//println("typ: "+typ)
					val ctor = typ.member(termNames.CONSTRUCTOR).asMethod
					val p0_l = ctor.paramLists(0)
					val nameToType_l = p0_l.map(p => p.name.decodedName.toString.replace("_?", "") -> p.typeSignature)
					for {
						nameToObj_m <- rjsval match {
							case m: RjsAbstractMap =>
								convMapString(m, nameToType_l)
							case RjsNull =>
								convMapString(RjsBasicMap(), nameToType_l)
							case _ =>
								ResultC.error(s"unhandled type or value. type=${typ}, value=${rjsval}")
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
		rjsval: RjsValue,
		typ2: Type
	): ResultC[List[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		rjsval match {
			case RjsList(v) =>
				ResultC.mapAll(v.zipWithIndex) { case (rjsval2, i0) =>
					val i = i0 + 1
					conv(rjsval2, typ2, Some(s"[$i]"))
				}
			case RjsNull =>
				ResultC.unit(Nil)
			case _ =>
				ResultC.or(
					conv(rjsval, typ2).map(List(_)),
					ResultC.error(s"expected an array of ${typ2.typeSymbol.name.toString}.  Instead found: ${rjsval}")
				)
		}
	}
	
	private def convSet(
		m: RjsAbstractMap,
		typ2: Type
	): ResultC[Set[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		// Try to convert each element of the array
		ResultC.mapAll(m.getValueMap.toList) ({ case (id, rjsval) =>
			conv(rjsval, typ2, Some(id))
		}).map(l => Set(l : _*))
	}

	private def convListToObject(
		rjsval_l: List[RjsValue],
		nameToType_l: List[(String, ru.Type)]
	): ResultC[Map[String, _]] = {
		ResultC.error("convListToObject: not yet implemented")
	}

	def convMap(
		m: RjsAbstractMap,
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
		val map = m.getValueMap
		
		// Try to convert each element of the object
		for {
			val_l <- ResultC.map(nameToType_l) { case (name, typ2) =>
				map.get(name) match {
					case Some(rjsval2) => conv(rjsval2, typ2, Some(name))
					// Field is missing, so try using RjsNull
					case None => conv(RjsNull, typ2, Some(name))
				}
			}
		} yield {
			(key_l zip val_l).toMap
		}
	}

	def convMapString(
		m: RjsAbstractMap,
		nameToType_l: List[(String, ru.Type)]
	): ResultC[Map[String, _]] = {
		for {
			map <- convMap(m, typeOf[String], nameToType_l)
		} yield map.asInstanceOf[Map[String, _]]
	}
	
	/*def yamlStringToRjsBasicValue(yaml: String): ResultC[RjsBasicValue] = {
		import spray.json._
		val json = JsonUtils.yamlToJsonText(yaml)
		val jsval = json.parseJson
		RjsValue.fromJson(jsval)
	}
	
	def yamlStringToRjs[A <: RjsValue : TypeTag](yaml: String): ResultC[RjsValue] = {
		for {
			rjsval0 <- ResultC.from(yamlStringToRjsBasicValue(yaml))
			rjsval <- fromRjs[A](rjsval0)
		} yield rjsval
	}*/

	def mergeObjects[A : TypeTag](o1: A, o2: A): ResultC[A] = {
		val typ = ru.typeTag[A].tpe
		for {
			o <- mergeObjects(o1, o2, typ)
			//_ <- ResultC.assert(o.isInstanceOf[A], s"INTERNAL: mis-converted JSON: `$rjsval` to `$o`")
		} yield o.asInstanceOf[A]
	}

	private def mergeObjects(o1: Any, o2: Any, typ: Type): ResultC[Any] = {
		import scala.reflect.runtime.universe._

		val mirror = runtimeMirror(this.getClass.getClassLoader)
		
		if (typ <:< typeOf[RjsValue]) {
			val v1 = o1.asInstanceOf[RjsValue]
			val v2 = o2.asInstanceOf[RjsValue]
			RjsValue.merge(v1, v2)
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
					rjsval1 <- RjsValue.fromObject(o1, typ)
					rjsval2 <- RjsValue.fromObject(o2, typ)
					rjsval3 <- RjsValue.merge(rjsval1, rjsval2)
					o <- conv(rjsval3, typ)
				} yield o
			}
		}
	}
}