package roboliq.input

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf
import grizzled.slf4j.Logger
import roboliq.ai.strips
import roboliq.utils.JsonUtils
import roboliq.core.ResultC

object RjsConverter {
	private val logger = Logger[this.type]
	
	def toString(rjsval: RjsValue): ResultE[String] = {
		
		def evalAndRetry(): ResultE[String] = {
			for {
				rjsval2 <- ResultE.evaluate(rjsval)
				s <- toString(rjsval2)
			} yield s
		}
		
		ResultE.context("toString") {
			rjsval match {
				case x: RjsString => ResultE.unit(x.s)
				case x: RjsText => ResultE.unit(x.text)
				case x: RjsFormat => evalAndRetry()
				case x: RjsSubst => evalAndRetry()
				case x: RjsCall => evalAndRetry()
				case x: RjsAbstractMap if x.typ_?.isDefined =>
					for {
						rjsval2 <- RjsValue.evaluateTypedMap(x)
						s <- toString(rjsval2)
					} yield s
				case _ =>
					ResultE.error(s"cannot convert to String: $rjsval")
			}
		}
	}
	
	def toInt(rjsval: RjsValue): ResultE[Int] = {
		toBigDecimal(rjsval).map(_.toInt)
	}
	
	def toInteger(rjsval: RjsValue): ResultE[Integer] = {
		toBigDecimal(rjsval).map(_.toInt)
	}
	
	def toFloat(rjsval: RjsValue): ResultE[Float] = {
		toBigDecimal(rjsval).map(_.toFloat)
	}
	
	def toDouble(rjsval: RjsValue): ResultE[Double] = {
		toBigDecimal(rjsval).map(_.toDouble)
	}
	
	def toBigDecimal(rjsval: RjsValue): ResultE[BigDecimal] = {
		ResultE.context("toBigDecimal") {
			rjsval match {
				case RjsNumber(n, None) => ResultE.unit(n)
				case RjsNumber(_, _) => ResultE.error("expected RjsNumber without units: "+rjsval.toText)
				case _ => ResultE.error("expected RjsNumber")
			}
		}
	}
	
	/*def toBigDecimal(scope: Map[String, RjsValue], name: String): ResultE[BigDecimal] = {
		scope.get(name) match {
			case None => ResultE.error(s"variable `$name` missing from scope")
			case Some(rjsval) => toBigDecimal(rjsval)
		}
	}*/
	
	def toBoolean(rjsval: RjsValue): ResultE[java.lang.Boolean] = {
		ResultE.context("toBoolean") {
			rjsval match {
				case RjsBoolean(b) => ResultE.unit(b)
				case RjsNumber(n, _) => ResultE.unit(n != 0)
				case RjsString(s) => ResultE.unit(s == "true")
				case _ => ResultE.error("expected RjsBoolean")
			}
		}
	}
	
	def toEnum[A <: Enumeration#Value : TypeTag](rjsval: RjsValue): ResultE[A] = {
		val typ = ru.typeTag[A].tpe
		toEnum(rjsval, typ).map(_.asInstanceOf[A])
	}
	
	private def toEnum(rjsval: RjsValue, typ: ru.Type): ResultE[Any] = {
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
					ResultE.from(value_l.find(_.toString == s), s"Value '$s' not valid for `${enumModule.name}`.  Expected one of ${value_l.mkString(", ")}.")
				case _ => ResultE.error("expected RjsString")
			}
		} catch {
			case e: Throwable => ResultE.error(s"type: $typ, rjsval: $rjsval, error: ${e.getMessage}")
		}
	}

	
	def toStripsLiteral(
		rjsval: RjsValue
	): ResultE[strips.Literal] = {
		for {
			s <- RjsConverter.toString(rjsval)
			lit <- s.split(" ").toList.filterNot(_.isEmpty) match {
				case Nil =>
					ResultE.error("expected a non-empty string for logical literal")
				case l =>
					val name0 = l.head
					val pos = !name0.startsWith("~")
					val name = if (pos) name0 else name0.substring(1)
					if (name.isEmpty) {
						ResultE.error(s"expected a non-empty name for logical literal in: $s")
					}
					else {
						ResultE.unit(strips.Literal(strips.Atom(name, l.tail), pos))
					}
			}
		} yield lit
	}
	
	def fromRjs[A: TypeTag](
		rjsval: RjsValue
	): ResultE[A] = {
		//println(s"fromRjson($rjsval)")
		val typ = ru.typeTag[A].tpe
		for {
			o <- conv(rjsval, typ)
			//_ <- ResultE.assert(o.isInstanceOf[A], s"INTERNAL: mis-converted JSON: `$rjsval` to `$o`")
		} yield o.asInstanceOf[A]
	}

	def fromRjs[A: TypeTag](map: RjsAbstractMap, field: String): ResultE[A] =
		fromRjs[A](map.getValueMap, field)

	def fromRjs[A: TypeTag](map: Map[String, RjsValue], field: String): ResultE[A] = {
		ResultE.context(field) {
			map.get(field) match {
				case Some(rjsval) => fromRjs[A](rjsval)
				case None =>
					ResultE.orElse(
						fromRjs[A](RjsNull),
						ResultE.error("value required")
					)
			}
		}
	}

	private def conv(
		rjsval: RjsValue,
		typ: Type,
		path_? : Option[String] = None
	): ResultE[Any] = {
		println(s"conv($rjsval, $typ)")
		import scala.reflect.runtime.universe._

		val mirror = runtimeMirror(this.getClass.getClassLoader)

		//val path = path_r.reverse.mkString(".")
		//val prefix = if (path_r.isEmpty) "" else path + ": "
		//logger.trace(s"conv(${path}, $rjsval, $typ, eb)")

		val ctx = {
			val ret: ResultE[Any] = {
				if (typ =:= typeOf[String]) RjsConverter.toString(rjsval)
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
					ResultE.unit(rjsval)
				}
				else if (typ <:< typeOf[RjsValue]) {
					if (mirror.runtimeClass(typ).isInstance(rjsval)) {
						ResultE.unit(rjsval)
					}
					else if (rjsval.isInstanceOf[RjsAbstractMap]) {
						val m = rjsval.asInstanceOf[RjsAbstractMap]
						if (typ =:= typeOf[RjsMap]) {
							ResultE.unit(RjsMap(m.getValueMap))
						}
						else if (typ =:= typeOf[RjsBasicMap]) {
							for {
								map <- ResultE.map(m.getValueMap.toList) { case (name, rjsval) =>
									for {
										basic <- ResultE.from(RjsValue.toBasicValue(rjsval))
									} yield name -> basic
								}
							} yield RjsBasicMap(map.toMap)
						}
						else {
							for {
								rjsval2 <- RjsValue.convertMap(m.getValueMap, typ)
								_ <- ResultE.assert(mirror.runtimeClass(typ).isInstance(rjsval2), s"Could not convert to TYPE=$typ from map ${m}")
							} yield rjsval2
							/*
							// As an alternative approach, consider creating a fromRjsValue companion method for those
							// types which can be converted from others 
							// If companion object has method 'fromRjsValue'
							val cs = typ.typeSymbol.companionSymbol
							cs.typeSignature.members.find(m => m.name.toString == "fromRjsValue")
							*/
						}
					}
					else {
						ResultE.error(s"Could not convert to TYPE=$typ from $rjsval")
					}
				}
				else if (typ <:< typeOf[Option[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					if (rjsval == RjsNull) ResultE.unit(None)
					else conv(rjsval, typ2).map(o => Option(o))
				}
				else if (typ <:< typeOf[Set[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					rjsval match {
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
						case RjsNull => ResultE.unit(Map())
						case _ =>
							ResultE.error("expected a RjsMap")
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
							case _ =>
								ResultE.error(s"unhandled type or value. type=${typ}, value=${rjsval}")
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
		rjsval: RjsValue,
		typ2: Type
	): ResultE[List[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		rjsval match {
			case RjsList(v) =>
				ResultE.mapAll(v.zipWithIndex) { case (rjsval2, i0) =>
					val i = i0 + 1
					conv(rjsval2, typ2, Some(s"[$i]"))
				}
			case RjsNull =>
				ResultE.unit(Nil)
			case _ =>
				ResultE.or(
					conv(rjsval, typ2).map(List(_)),
					ResultE.error(s"expected an array of ${typ2.typeSymbol.name.toString}.  Instead found: ${rjsval}")
				)
		}
	}
	
	private def convSet(
		m: RjsAbstractMap,
		typ2: Type
	): ResultE[Set[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		// Try to convert each element of the array
		ResultE.mapAll(m.getValueMap.toList) ({ case (id, rjsval) =>
			conv(rjsval, typ2, Some(id))
		}).map(l => Set(l : _*))
	}

	private def convListToObject(
		rjsval_l: List[RjsValue],
		nameToType_l: List[(String, ru.Type)]
	): ResultE[Map[String, _]] = {
		ResultE.error("convListToObject: not yet implemented")
	}

	def convMap(
		m: RjsAbstractMap,
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
		val map = m.getValueMap
		
		// Try to convert each element of the object
		for {
			val_l <- ResultE.map(nameToType_l) { case (name, typ2) =>
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
	): ResultE[Map[String, _]] = {
		for {
			map <- convMap(m, typeOf[String], nameToType_l)
		} yield map.asInstanceOf[Map[String, _]]
	}
	
	def yamlStringToRjsBasicValue(yaml: String): ResultC[RjsBasicValue] = {
		import spray.json._
		val json = JsonUtils.yamlToJsonText(yaml)
		val jsval = json.parseJson
		RjsValue.fromJson(jsval)
	}
	
	def yamlStringToRjs[A <: RjsValue : TypeTag](yaml: String): ResultE[RjsValue] = {
		for {
			rjsval0 <- ResultE.from(yamlStringToRjsBasicValue(yaml))
			rjsval <- fromRjs[A](rjsval0)
		} yield rjsval
	}
}