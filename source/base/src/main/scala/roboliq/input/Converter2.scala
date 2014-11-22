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

	def toString(jsval: JsValue): ContextT[String] = {
		ContextT.context("toString") {
			jsval match {
				case JsString(text) => ContextT.unit(text)
				case JsObject(m) =>
					for {
						_ <- ContextT.assert(m.get("TYPE") == "string", s"expected JsString or TYPE=string")
						jsval2 <- ContextT.from(m.get("VALUE"), s"expected VALUE for string")
						x <- toString(jsval2)
					} yield x
				case _ => ContextT.error("expected JsString")
			}
		}
	}
	
	def toInt(jsval: JsValue): ContextT[Int] = {
		toBigDecimal(jsval).map(_.toInt)
	}
	
	def toInteger(jsval: JsValue): ContextT[Integer] = {
		toBigDecimal(jsval).map(_.toInt)
	}
	
	def toDouble(jsval: JsValue): ContextT[Double] = {
		toBigDecimal(jsval).map(_.toDouble)
	}
	
	def toBigDecimal(jsval: JsValue): ContextT[BigDecimal] = {
		ContextT.context("toBigDecimal") {
			jsval match {
				case JsObject(m) =>
					(m.get("TYPE"), m.get("VALUE")) match {
						case (Some(JsString("number")), Some(JsNumber(n))) =>
							ContextT.unit(n)
						case _ =>
							ContextT.error("Expected TYPE=number and VALUE of type JsNumber: "+jsval)
					}
				case JsNumber(n) => ContextT.unit(n)
				case _ => ContextT.error("expected JsNumber or JsObject")
			}
		}
	}
	
	def toBigDecimal(scope: Map[String, JsValue], name: String): ContextT[BigDecimal] = {
		scope.get(name) match {
			case None => ContextT.error(s"variable `$name` missing from scope")
			case Some(jsval) => toBigDecimal(jsval)
		}
	}
	
	def toBoolean(jsval: JsValue): ContextT[java.lang.Boolean] = {
		ContextT.context("toBoolean") {
			jsval match {
				case JsObject(m) =>
					for {
						_ <- ContextT.assert(m.get("TYPE") == "boolean", s"expected JsBoolean or TYPE=boolean")
						jsval2 <- ContextT.from(m.get("VALUE"), s"expected VALUE for boolean")
						x <- toBoolean(jsval2)
					} yield x
				case JsBoolean(b) => ContextT.unit(b)
				case _ => ContextT.error("expected JsBoolean")
			}
		}
	}
	
	def makeNumber(n: BigDecimal): JsObject = {
		JsObject(Map("TYPE" -> JsString("number"), "VALUE" -> JsNumber(n)))
	}
	
	def makeCall(name: String, input: Map[String, JsValue]): JsObject = {
		JsObject(Map("TYPE" -> JsString("call"), "VALUE" -> JsString(name), "INPUT" -> JsObject(input)))
	}

	private def conv(
		path_r: List[String],
		jsval: JsValue,
		typ: Type
	): ContextT[Any] = {
		import scala.reflect.runtime.universe._

		val mirror = runtimeMirror(this.getClass.getClassLoader)

		val path = path_r.reverse.mkString(".")
		val prefix = if (path_r.isEmpty) "" else path + ": "
		logger.trace(s"conv(${path}, $jsval, $typ, eb)")

		ContextT.context(path) {
			val ret: ContextT[Any] = {
				if (typ =:= typeOf[JsValue]) ContextT.unit(jsval)
				else if (typ =:= typeOf[String]) Converter2.toString(jsval)
				else if (typ =:= typeOf[Int]) toInt(jsval)
				else if (typ =:= typeOf[Integer]) toInteger(jsval)
				else if (typ =:= typeOf[Double]) toDouble(jsval)
				else if (typ =:= typeOf[BigDecimal]) toBigDecimal(jsval)
				else if (typ =:= typeOf[Boolean]) toBoolean(jsval)//.map(_.asInstanceOf[Boolean])
				else if (typ =:= typeOf[java.lang.Boolean]) toBoolean(jsval)
				/*
				else if (typ <:< typeOf[Enumeration#Value]) toEnum(jsval, typ)
				else if (typ =:= typeOf[AmountSpec]) toAmountSpec(jsval)
				else if (typ =:= typeOf[LiquidSource]) toLiquidSource(jsval, eb, state_?)
				else if (typ =:= typeOf[LiquidVolume]) toVolume(jsval)
				else if (typ =:= typeOf[PipetteAmount]) toPipetteAmount(jsval)
				else if (typ =:= typeOf[PipetteDestination]) toPipetteDestination(jsval, eb, state_?)
				else if (typ =:= typeOf[PipetteDestinations]) toPipetteDestinations(jsval, eb, state_?)
				else if (typ =:= typeOf[PipetteSources]) toPipetteSources(jsval, eb, state_?)
				// Logic
				else if (typ =:= typeOf[Strips.Literal]) toStripsLiteral(jsval)
				// Lookups
				else if (typ =:= typeOf[Agent]) toEntityByRef[Agent](jsval, eb)
				else if (typ =:= typeOf[Labware]) toEntityByRef[Labware](jsval, eb)
				else if (typ =:= typeOf[Pipetter]) toEntityByRef[Pipetter](jsval, eb)
				else if (typ =:= typeOf[Shaker]) toEntityByRef[Shaker](jsval, eb)
				else if (typ =:= typeOf[TipModel]) toEntityByRef[TipModel](jsval, eb)
				//else if (typ <:< typeOf[Substance]) toSubstance(jsval)
				*/
				else if (typ <:< typeOf[Option[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					if (jsval == JsNull) ContextT.unit(None)
					else conv(path_r, jsval, typ2).map(_ match {
						case ConvObject(o) => ConvObject(Option(o))
						case res => res
					})
				}
				else if (typ <:< typeOf[List[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					convList(path_r, jsval, typ2)
				}
				else if (typ <:< typeOf[Set[_]]) {
					val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
					jsval match {
						case jsobj @ JsObject(fields) =>
							convSet(path_r, jsobj, typ2)
						case _ =>
							convList(path_r, jsval, typ2).map(l => Set(l : _*))
					}
				}
				else if (typ <:< typeOf[Map[_, _]]) {
					jsval match {
						case jsobj @ JsObject(fields) =>
							println("fields: " + fields)
							val typKey = typ.asInstanceOf[ru.TypeRefApi].args(0)
							val typVal = typ.asInstanceOf[ru.TypeRefApi].args(1)
							val name_l = fields.toList.map(_._1)
							val nameToType_l = name_l.map(_ -> typVal)
							for {
								res <- convMap(path_r, jsobj, typKey, nameToType_l)
							} yield res
						case JsNull => ContextT.unit(Map())
						case _ =>
							ContextT.error("expected a JsObject")
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
								convMapString(path_r, jsobj, nameToType_l)
							case JsArray(jsval_l) =>
								convListToObject(path_r, jsval_l, nameToType_l)
							/*case JsString(s) =>
								eb.getEntity(s) match {
									case Some(obj) =>
										// FIXME: need to check type rather than just assuming that it's correct!
										ContextT.unit(Left(ConvObject(obj)))
									case None =>
										for {
											nameToVal_l <- parseStringToArgs(s)
											//_ = println("nameToVal_l: "+nameToVal_l.toString)
											res <- convArgsToMap(path_r, nameToVal_l, typ, nameToType_l, eb, state_?, id_?)
											//_ = println("res: "+res.toString)
										} yield res
								}
							*/
							case _ =>
								convListToObject(path_r, List(jsval), nameToType_l)
							//case _ =>
							//	ContextT.error(s"unhandled type or value. type=${typ}, value=${jsval}")
						}
					} yield {
						val arg_l = nameToType_l.map(pair => nameToObj_m(pair._1))
						val c = typ.typeSymbol.asClass
						//println("arg_l: "+arg_l)
						val mm = mirror.reflectClass(c).reflectConstructor(ctor)
						logger.debug("arg_l: "+arg_l)
						val obj = mm(arg_l : _*)
						obj
					}
				}
			}
			logger.debug(ret)
			ret
		}
	}

		
	private def convList(
		path_r: List[String],
		jsval: JsValue,
		typ2: Type
	): ContextT[List[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		jsval match {
			case JsArray(v) =>
				ContextT.mapAll(v.zipWithIndex) { case (jsval2, i0) =>
					val i = i0 + 1
					val path2_r = path_r match {
						case Nil => List(s"[$i]")
						case head :: rest => (s"$head[$i]") :: rest
					}
					conv(path2_r, jsval2, typ2)
				}
			case JsNull =>
				ContextT.unit(Nil)
			case _ =>
				ContextT.or(
					conv(path_r, jsval, typ2).map(List(_)),
					ContextT.error(s"expected an array of ${typ2.typeSymbol.name.toString}.  Instead found: ${jsval}")
				)
		}
	}
	
	private def convSet(
		path_r: List[String],
		jsobj: JsObject,
		typ2: Type
	): ContextT[Set[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		// Try to convert each element of the array
		ContextT.mapAll(jsobj.fields.toList) ({ case (id, jsval) =>
			conv(id :: path_r, jsval, typ2)
		}).map(l => Set(l : _*))
	}

	private def convListToObject(
		path_r: List[String],
		jsval_l: List[JsValue],
		nameToType_l: List[(String, ru.Type)]
	): ContextT[Map[String, _]] = {
		ContextT.error("convListToObject: not yet implemented")
	}

	private def convMap(
		path_r: List[String],
		jsobj: JsObject,
		typKey: Type,
		nameToType_l: List[(String, ru.Type)]
	): ContextT[Map[_, _]] = {
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
			val_l <- ContextT.map(nameToType_l) { case (name, typ2) =>
				val path2_r = name :: path_r
				jsobj.fields.get(name) match {
					case Some(jsval2) => conv(path2_r, jsval2, typ2)
					// Field is missing, so try using JsNull
					case None => conv(path2_r, JsNull, typ2)
				}
			}
		} yield {
			(key_l zip val_l).toMap
		}
	}

	private def convMapString(
		path_r: List[String],
		jsobj: JsObject,
		nameToType_l: List[(String, ru.Type)]
	): ContextT[Map[String, _]] = {
		for {
			map <- convMap(path_r, jsobj, typeOf[String], nameToType_l)
		} yield map.asInstanceOf[Map[String, _]]
	}
}