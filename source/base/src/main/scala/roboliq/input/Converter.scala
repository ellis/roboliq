package roboliq.input

import scala.language.implicitConversions
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf
import grizzled.slf4j.Logger
import spray.json._
import roboliq.core._
import roboliq.entities._

case class KeyClassOpt(
	key: String,
	typ: Type,
	opt: Boolean = false
) {
	/*def changeKey(key: String): KeyClassOpt =
		this.copy(kc = kc.changeKey(key))
	def changeClassToJsValue: KeyClassOpt =
		this.copy(kc = kc.changeClassToJsValue)
	def changeTime(time: List[Int]): KeyClassOpt =
		this.copy(kc = kc.changeTime(time))*/
}

private sealed trait ConvResult {
	def +(that: ConvResult): ConvResult
}
private case class ConvObject(o: Any) extends ConvResult {
	def +(that: ConvResult): ConvResult = {
		that
	}
}

private case class ConvInfo[A: TypeTag](
	fnToJson: A => RqResult[JsValue],
	fromJson: (List[String], JsValue, ru.Type, EntityBase) => RqResult[ConvResult]
) {
	val typ = ru.typeTag[A].tpe
	def toJson(obj: Any) = fnToJson(obj.asInstanceOf[A])
}

private case class ConvRequire(require_m: Map[String, KeyClassOpt]) extends ConvResult {
	def +(that: ConvResult): ConvResult = {
		that match {
			case _: ConvObject => this
			case ConvRequire(m) => ConvRequire(require_m ++ m)
		}
	}
}

object Converter {
	private val logger = Logger[this.type]
	
	private val convInfo_l = List[ConvInfo[_]](
		ConvInfo[CleanIntensity.Value](
			(o: CleanIntensity.Value) => {
				RsSuccess(JsString(o match {
					case CleanIntensity.None => "none"
					case CleanIntensity.Flush => "flush"
					case CleanIntensity.Light => "light"
					case CleanIntensity.Thorough => "thorough"
					case CleanIntensity.Decontaminate => "decontaminate"
				}))
			},
			(path_r: List[String], jsval: JsValue, typ: ru.Type, eb: EntityBase) => {
				jsval match {
					case JsString(text) => text match {
						case "none" => RqSuccess(ConvObject(CleanIntensity.None))
						case "flush" => RqSuccess(ConvObject(CleanIntensity.Flush))
						case "light" => RqSuccess(ConvObject(CleanIntensity.Light))
						case "thorough" => RqSuccess(ConvObject(CleanIntensity.Thorough))
						case "decontaminate" => RqSuccess(ConvObject(CleanIntensity.Decontaminate))
					}
					case _ => RqError("expected JsString")
				}
			}
		),
		ConvInfo[PipettePosition.Value](
			(o: PipettePosition.Value) => {
				RsSuccess(JsString(o match {
					case PipettePosition.Free => "air"
					case PipettePosition.WetContact => "wet"
					case PipettePosition.DryContact => "dry"
				}))
			},
			(path_r: List[String], jsval: JsValue, typ: ru.Type, eb: EntityBase) => {
				jsval match {
					case JsString(text) => text match {
						case "air" => RqSuccess(ConvObject(PipettePosition.Free))
						case "wet" => RqSuccess(ConvObject(PipettePosition.WetContact))
						case "dry" => RqSuccess(ConvObject(PipettePosition.DryContact))
					}
					case _ => RqError("expected JsString")
				}
			}
		),
		ConvInfo[TipCleanPolicy](
			(o: TipCleanPolicy) => {
				RqSuccess(JsString(s"${o.enter}${o.exit}"))
			},
			(path_r: List[String], jsval: JsValue, typ: ru.Type, eb: EntityBase) => {
				jsval match {
					case JsString(text) => text match {
						case "None" => RqSuccess(ConvObject(TipCleanPolicy.NN))
						case "ThoroughNone" => RqSuccess(ConvObject(TipCleanPolicy.TN))
						case "ThoroughLight" => RqSuccess(ConvObject(TipCleanPolicy.TL))
						case "Thorough" => RqSuccess(ConvObject(TipCleanPolicy.TT))
						case "Decontaminate" => RqSuccess(ConvObject(TipCleanPolicy.DD))
						case _ => RqError("unrecognized TipCleanPolicy")
					}
					case _ => RqError("expected JsString")
				}
			}
		)
	)

	/*val entityType_l = Set[Type](
		ru.typeTag[].tpe
	)*/
	private def findTableForType(typ: Type): RsResult[Unit] = {
		if (typ <:< typeOf[Entity]) RsSuccess(())
		else RsError("INTERNAL")
	}
	
	def convAs[A: TypeTag](
		jsval: JsValue,
		eb: EntityBase,
		state_? : Option[WorldState]
	): RqResult[A] = {
		val typ = ru.typeTag[A].tpe
		convOrRequire(Nil, jsval, typ, eb, state_?).flatMap(_ match {
			case ConvRequire(m) => RqError("need to lookup values for "+m.keys.mkString(", "))
			case ConvObject(o) => RqSuccess(o.asInstanceOf[A])
		})
	}
	
	def convCommandAs[A <: commands.Command : TypeTag](
		nameToVal_l: List[(Option[String], JsValue)],
		eb: EntityBase,
		state: WorldState
	): RqResult[A] = {
		import scala.reflect.runtime.universe._

		val typ = ru.typeTag[A].tpe
		for {
			res <- convArgs(nameToVal_l, typ, eb, Some(state))
		} yield res.asInstanceOf[A]
	}
	
	def convActionAs[A: TypeTag](
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase
	): RqResult[A] = {
		import scala.reflect.runtime.universe._

		val nameToVal_l = paramToJsval_l.map(pair => Some(pair._1) -> pair._2)
		val typ = ru.typeTag[A].tpe
		for {
			res <- convArgs(nameToVal_l, typ, eb, None)
		} yield res.asInstanceOf[A]
	}
	
	def convInstructionAs[A: TypeTag](
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state: WorldState
	): RqResult[A] = {
		import scala.reflect.runtime.universe._

		val nameToVal_l = paramToJsval_l.map(pair => Some(pair._1) -> pair._2)
		val typ = ru.typeTag[A].tpe
		for {
			res <- convArgs(nameToVal_l, typ, eb, Some(state))
		} yield res.asInstanceOf[A]
	}
	
	def convArgs(
		nameToVal_l: List[(Option[String], JsValue)],
		typ: ru.Type,
		eb: EntityBase,
		state_? : Option[WorldState]
	): RqResult[Any] = {
		import scala.reflect.runtime.universe._

		val ctor = typ.member(nme.CONSTRUCTOR).asMethod
		val p0_l = ctor.paramss(0)
		val nameToType_l = p0_l.map(p => p.name.decoded.replace("_?", "") -> p.typeSignature)

		def doit(
			nameToType_l: List[(String, Type)],
			jsval_l: List[JsValue],
			nameToVal_m: Map[String, JsValue],
			acc_r: List[JsValue]
		): RqResult[List[JsValue]] = {
			nameToType_l match {
				case Nil =>
					// TODO: return warning for any extra parameters
					RsSuccess(acc_r.reverse)
				case nameToType :: nameToType_l_~ =>
					val (name, typ) = nameToType
					// Check whether named parameter is provided
					nameToVal_m.get(name) match {
						case Some(jsval) =>
							val nameToVal_m_~ = nameToVal_m - name
							doit(nameToType_l_~, jsval_l, nameToVal_m_~, jsval :: acc_r)
						case None =>
							jsval_l match {
								// Use unnamed parameter
								case jsval :: jsval_l_~ =>
									doit(nameToType_l_~, jsval_l_~, nameToVal_m, jsval :: acc_r)
								// Else parameter value is blank
								case Nil =>
									doit(nameToType_l_~, jsval_l, nameToVal_m, JsNull :: acc_r)
							}
					}
			}
		}

		val jsval_l = nameToVal_l.collect({case (None, jsval) => jsval})
		val nameToVal2_l: List[(String, JsValue)] = nameToVal_l.collect({case (Some(name), jsval) => (name, jsval)})
		val nameToVals_m: Map[String, List[(String, JsValue)]] = nameToVal2_l.groupBy(_._1)
		val nameToVals_l: List[(String, List[JsValue])] = nameToVals_m.toList.map(pair => pair._1 -> pair._2.map(_._2))
		
		for {
			nameToVal3_l <- RsResult.toResultOfList(nameToVals_l.map(pair => {
				val (name, jsval_l) = pair
				jsval_l match {
					case jsval :: Nil => RsSuccess((name, jsval))
					case _ => RsError(s"too many values supplied for argument `$name`")
				}
			}))
			nameToVal_m = nameToVal3_l.toMap
			l <- doit(nameToType_l, jsval_l, nameToVal_m, Nil)
			res <- conv(JsArray(l), typ, eb, state_?)
		} yield res
	}
	
	def conv(
		jsval: JsValue,
		typ: ru.Type,
		eb: EntityBase,
		state_? : Option[WorldState] = None
	): RqResult[Any] = {
		convOrRequire(Nil, jsval, typ, eb, state_?).flatMap(_ match {
			case ConvRequire(m) => RqError("need to lookup values for "+m.keys.mkString(", "))
			case ConvObject(o) => RqSuccess(o)
		})
	}

	private def convOrRequire(
		path_r: List[String],
		jsval: JsValue,
		typ: Type,
		eb: EntityBase,
		state_? : Option[WorldState],
		id_? : Option[String] = None
	): RsResult[ConvResult] = {
		import scala.reflect.runtime.universe._

		val mirror = runtimeMirror(this.getClass.getClassLoader)

		val path = path_r.reverse.mkString(".")
		val prefix = if (path_r.isEmpty) "" else path + ": "
		logger.trace(s"conv(${path}, $jsval, $typ, eb)")

		try {
			jsval match {
				case JsString(s) =>
					// An initial "*" indicates a reference to another object by ID.
					val ref_? = {
						if (s.startsWith("*"))
							Some(s.tail)
						else if (findTableForType(typ).isSuccess)
							Some(s)
						else
							None
					}
					if (ref_?.isDefined) {
						return {
							eb.getEntity(ref_?.get) match {
								case Some(o) => RqSuccess(ConvObject(o))
								case None => RqError(s"value at `$path = $s` not found")
							}
							/*
							// Otherwise create a list of required objects
							case None =>
								findTableForType(typ).map { table =>
									val id = id_?.get
									val tkp = TKP(table, id, Nil)
									val kco = KeyClassOpt(KeyClass(tkp, typ, time), false, None)
									ConvRequire(Map(path -> kco))
								}
							*/
						}
					}
				case _ =>
			}
			
			//def changeResult(res: RqResult[Any]): RqResult[ConvResult] = res.map(ConvObject)
			
			def addPrefix(l: List[String]): List[String] = l.flatMap(message => List(message, s"given type: ${jsval.getClass()}", s"given value: $jsval").map(prefix + _))
			implicit def withPath(result: RqResult[Any]): RqResult[ConvResult] = {
				result match {
					case RqSuccess(x, w) => RqSuccess(ConvObject(x), addPrefix(w))
					case RqError(e, w) => RqError(addPrefix(e), addPrefix(w))
				}
			}
			
			val convInfo_? = convInfo_l.find(typ <:< _.typ)
			val ret: RsResult[ConvResult] =
			if (convInfo_?.isDefined) convInfo_?.get.fromJson(path_r, jsval, typ, eb)
			else if (typ =:= typeOf[String]) Converter.toString(jsval)
			else if (typ =:= typeOf[Int]) toInt(jsval)
			else if (typ =:= typeOf[Integer]) toInteger(jsval)
			else if (typ =:= typeOf[Double]) toDouble(jsval)
			else if (typ =:= typeOf[BigDecimal]) toBigDecimal(jsval)
			else if (typ =:= typeOf[Boolean]) toBoolean(jsval)//.map(_.asInstanceOf[Boolean])
			else if (typ =:= typeOf[java.lang.Boolean]) toBoolean(jsval)
			else if (typ <:< typeOf[Enumeration#Value]) toEnum(jsval, typ)
			else if (typ =:= typeOf[LiquidVolume]) toVolume(jsval)
			else if (typ =:= typeOf[PipetteAmount]) toPipetteAmount(jsval)
			else if (typ =:= typeOf[PipetteDestinations]) toPipetteDestinations(jsval, eb, state_?)
			else if (typ =:= typeOf[PipetteSources]) toPipetteSources(jsval, eb, state_?)
			else if (typ =:= typeOf[Pipetter]) toEntityByRef[Pipetter](jsval, eb)
			else if (typ =:= typeOf[TipModel]) toEntityByRef[TipModel](jsval, eb)
			//else if (typ <:< typeOf[Substance]) toSubstance(jsval)
			else if (typ <:< typeOf[Option[_]]) {
				val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
				if (jsval == JsNull) RqSuccess(ConvObject(None))
				else convOrRequire(path_r, jsval, typ2, eb, state_?).map(_ match {
					case ConvObject(o) => ConvObject(Option(o))
					case res => res
				})
			}
			else if (typ <:< typeOf[List[_]]) {
				val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
				convList(path_r, jsval, typ2, eb, state_?)
			}
			else if (typ <:< typeOf[Set[_]]) {
				val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
				jsval match {
					case jsobj @ JsObject(fields) =>
						convSet(path_r, jsobj, typ2, eb, state_?)
					case _ =>
						convList(path_r, jsval, typ2, eb, state_?).map(_ match {
							case ConvObject(l: List[_]) => ConvObject(Set(l : _*))
							case r => r
						})
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
							res <- convMap(path_r, jsobj, typKey, nameToType_l, eb, state_?, id_?)
						} yield {
							res match {
								case Right(map) => ConvObject(map)
								case Left(req) => req
							}
						}
					case JsNull => RqSuccess(ConvObject(Map()))
					case _ =>
						RqError("expected a JsObject")
				}
			}
			else {
				//println("typ: "+typ)
				val ctor = typ.member(nme.CONSTRUCTOR).asMethod
				val p0_l = ctor.paramss(0)
				val nameToType_l = p0_l.map(p => p.name.decoded.replace("_?", "") -> p.typeSignature)
				val res = jsval match {
					case jsobj: JsObject =>
						convMapString(path_r, jsobj, nameToType_l, eb, state_?, id_?)
					case JsArray(jsval_l) =>
						convListToObject(path_r, jsval_l, nameToType_l, eb, state_?, id_?)
					case JsString(s) =>
						for {
							nameToVal_l <- parseStringToArgs(s)
							//_ = println("nameToVal_l: "+nameToVal_l.toString)
							res <- convArgsToMap(path_r, nameToVal_l, typ, nameToType_l, eb, state_?, id_?)
							//_ = println("res: "+res.toString)
						} yield res
					case _ =>
						convListToObject(path_r, List(jsval), nameToType_l, eb, state_?, id_?)
					//case _ =>
					//	RqError(s"unhandled type or value. type=${typ}, value=${jsval}")
				}
				res.map(_ match {
					case Right(nameToObj_m) =>
						val arg_l = nameToType_l.map(pair => nameToObj_m(pair._1))
						val c = typ.typeSymbol.asClass
						//println("arg_l: "+arg_l)
						val mm = mirror.reflectClass(c).reflectConstructor(ctor)
						logger.debug("arg_l: "+arg_l)
						val obj = mm(arg_l : _*)
						ConvObject(obj)
					case Left(r) => r
				})
			}
			logger.debug(ret)
			ret
		}
		catch {
			case e: Throwable => //e.RqError(s"error converting `$path`: "+e.getStackTrace())
				throw e
		}
	}
	
	private def convList(
		path_r: List[String],
		jsval: JsValue,
		typ2: Type,
		eb: EntityBase,
		state_? : Option[WorldState]
	): RsResult[ConvResult] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		jsval match {
			case JsArray(v) =>
				// Try to convert each element of the array
				val res0 = RqResult.toResultOfList(v.zipWithIndex.map(pair => {
					val (jsval2, i) = pair
					val path2_r = path_r match {
						case Nil => List(s"[$i]")
						case head :: rest => (s"$head[$i]") :: rest
					}
					convOrRequire(path2_r, jsval2, typ2, eb, state_?)
				}))
				// If there were no errors in conversion,
				res0.map(l => {
					// If there are any requirements, return a list of all requirements.
					if (l.exists(_.isInstanceOf[ConvRequire])) {
						ConvRequire(l.collect({case ConvRequire(m) => m}).flatten.toMap)
					}
					// Otherwise, return list of objects.
					else {
						ConvObject(l.collect({case ConvObject(o) => o}))
					}
				})
			case JsNull =>
				RsSuccess(ConvObject(Nil))
			case _ =>
				convOrRequire(path_r, jsval, typ2, eb, state_?).map(_ match {
					case x: ConvRequire => x
					case ConvObject(o) => ConvObject(List(o))
				}).orElse(RsError(List(s"expected an array of ${typ2.typeSymbol.name.toString}", s"instead got: ${jsval}", eb.getEntity("water").toString)))
		}
	}
	
	private def convSet(
		path_r: List[String],
		jsobj: JsObject,
		typ2: Type,
		eb: EntityBase,
		state_? : Option[WorldState]
	): RsResult[ConvResult] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		// Try to convert each element of the array
		val res0 = RqResult.toResultOfList(jsobj.fields.toList.map(pair => {
			val (id, jsval) = pair
			val path_r_~ = id :: path_r
			convOrRequire(path_r_~, jsval, typ2, eb, state_?, Some(id))
		}))
		// If there were no errors in conversion,
		res0.map(l => {
			// If there are any requirements, return a list of all requirements.
			if (l.exists(_.isInstanceOf[ConvRequire])) {
				ConvRequire(l.collect({case ConvRequire(m) => m}).flatten.toMap)
			}
			// Otherwise, return list of objects.
			else {
				ConvObject(l.collect({case ConvObject(o) => o}).toSet)
			}
		})
	}
	
	private def convListToObject(
		path_r: List[String],
		jsval_l: List[JsValue],
		nameToType_l: List[(String, ru.Type)],
		eb: EntityBase,
		state_? : Option[WorldState],
		id_? : Option[String]
	): RqResult[Either[ConvRequire, Map[String, _]]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		// Try to convert each element of the object
		val (errV_l, wV, convV_l, val_l):
			(List[String], List[String], Map[String, KeyClassOpt], List[_]) = {
			var jsval_l_~ = jsval_l
			val res0 = RqResult.toResultOfList(nameToType_l.map(pair => {
				val (name, typ2) = pair
				val path2_r = name :: path_r
				if (name == "id" && id_?.isDefined)
					RsSuccess(ConvObject(id_?.get))
				else {
					jsval_l_~ match {
						case jsval :: rest =>
							jsval_l_~ = rest
							convOrRequire(path2_r, jsval, typ2, eb, state_?, Some(name))
						// Else try using JsNull
						case Nil =>
							convOrRequire(path2_r, JsNull, typ2, eb, state_?, Some(name))
					}
				}
			}))
			res0 match {
				case RqError(e, w) => (e, w, Map(), Nil)
				case RqSuccess(l, w) =>
					val conv_l = l.collect({case ConvRequire(m) => m}).flatten.toMap
					val obj_l = l.collect({case ConvObject(o) => o})
					(Nil, w, conv_l, obj_l)
			}
		}

		val key_l = nameToType_l.map(_._1)
		val err_l = errV_l
		val warning_l = wV
		err_l match {
			// No errors
			case Nil =>
				val conv_l = convV_l
				// Nothing to look up
				if (conv_l.isEmpty) {
					RqSuccess(Right((key_l zip val_l).toMap), warning_l)
				}
				else {
					RqSuccess(Left(ConvRequire(conv_l)), warning_l)
				}
			case _ =>
				RqError(err_l, warning_l)
		}
	}

	private def convMap(
		path_r: List[String],
		jsobj: JsObject,
		typKey: Type,
		nameToType_l: List[(String, ru.Type)],
		eb: EntityBase,
		state_? : Option[WorldState],
		id_? : Option[String]
	): RqResult[Either[ConvRequire, Map[_, _]]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)
		
		println("convMap: ")
		println(path_r)
		println(jsobj)
		println(typKey)
		println(nameToType_l)

		// Handle keys if they need to be looked up
		val (errK_l, wK, convK_l, key_l):
			(List[String], List[String], Map[String, KeyClassOpt], List[_]) =
			findTableForType(typKey) match {
				case RqSuccess(_, _) =>
					val res0 = RqResult.toResultOfList(nameToType_l.map(pair => {
						val (id, _) = pair
						val path2_r = (id + "#") :: path_r
						convOrRequire(path2_r, JsString(id), typKey, eb, state_?)
					}))
					res0 match {
						case RqError(e, w) => (e, w, Map(), Nil)
						case RqSuccess(l, w) =>
							val conv_l = l.collect({case ConvRequire(m) => m}).flatten.toMap
							val obj_l = l.collect({case ConvObject(o) => o})
							(Nil, w, conv_l, obj_l)
					}
				case _ =>
					(Nil, Nil, Map(), nameToType_l.map(_._1))
			}
		
		// Try to convert each element of the object
		val (errV_l, wV, convV_l, val_l):
			(List[String], List[String], Map[String, KeyClassOpt], List[_]) = {
			val res0 = RqResult.toResultOfList(nameToType_l.map(pair => {
				val (name, typ2) = pair
				val path2_r = name :: path_r
				jsobj.fields.get(name) match {
					case Some(jsval2) => convOrRequire(path2_r, jsval2, typ2, eb, state_?, Some(name))
					// Field is missing
					case None =>
						// If this is the special ID field, and an ID was passed:
						if (name == "id" && id_?.isDefined)
							RsSuccess(ConvObject(id_?.get))
						else if (name == "key" && typ2 =:= typeOf[String])
							RsSuccess(ConvObject(java.util.UUID.randomUUID().toString()))
						// Else try using JsNull
						else
							convOrRequire(path2_r, JsNull, typ2, eb, state_?, Some(name))
				}
			}))
			res0 match {
				case RqError(e, w) => (e, w, Map(), Nil)
				case RqSuccess(l, w) =>
					val conv_l = l.collect({case ConvRequire(m) => m}).flatten.toMap
					val obj_l = l.collect({case ConvObject(o) => o})
					(Nil, w, conv_l, obj_l)
			}
		}

		val err_l = errK_l ++ errV_l
		val warning_l = wK ++ wV
		err_l match {
			// No errors
			case Nil =>
				val conv_l = convV_l ++ convK_l
				// Nothing to look up
				if (conv_l.isEmpty) {
					RqSuccess(Right((key_l zip val_l).toMap), warning_l)
				}
				else {
					RqSuccess(Left(ConvRequire(conv_l)), warning_l)
				}
			case _ =>
				RqError(err_l, warning_l)
		}
	}

	private def convMapString(
		path_r: List[String],
		jsobj: JsObject,
		nameToType_l: List[(String, ru.Type)],
		eb: EntityBase,
		state_? : Option[WorldState],
		id_? : Option[String]
	): RqResult[Either[ConvRequire, Map[String, _]]] = {
		for {
			res <- convMap(path_r, jsobj, typeOf[String], nameToType_l, eb, state_?, id_?)
		} yield {
			res match {
				case Right(map) => Right(map.asInstanceOf[Map[String, _]])
				case Left(x) => Left(x)
			}
		}
	}
	
	private def convArgsToMap(
		path_r: List[String],
		nameToVal_l: List[(Option[String], JsValue)],
		typ: ru.Type,
		nameToType_l: List[(String, ru.Type)],
		eb: EntityBase,
		state_? : Option[WorldState],
		id_? : Option[String]
	): RqResult[Either[ConvRequire, Map[String, _]]] = {
		import scala.reflect.runtime.universe._

		def doit(
			nameToType_l: List[(String, Type)],
			jsval_l: List[JsValue],
			nameToVal_m: Map[String, JsValue],
			acc: Map[String, JsValue]
		): RqResult[Map[String, JsValue]] = {
			nameToType_l match {
				case Nil =>
					// TODO: return warning for any extra parameters
					RsSuccess(acc)
				case nameToType :: nameToType_l_~ =>
					val (name, typ) = nameToType
					// Check whether named parameter is provided
					nameToVal_m.get(name) match {
						case Some(jsval) =>
							val nameToVal_m_~ = nameToVal_m - name
							doit(nameToType_l_~, jsval_l, nameToVal_m_~, acc + (name -> jsval))
						case None =>
							jsval_l match {
								// Use unnamed parameter
								case jsval :: jsval_l_~ =>
									doit(nameToType_l_~, jsval_l_~, nameToVal_m, acc + (name -> jsval))
								// Else parameter value is blank
								case Nil =>
									doit(nameToType_l_~, jsval_l, nameToVal_m, acc)
							}
					}
			}
		}

		val jsval_l = nameToVal_l.collect({case (None, jsval) => jsval})
		val nameToVal2_l: List[(String, JsValue)] = nameToVal_l.collect({case (Some(name), jsval) => (name, jsval)})
		val nameToVals_m: Map[String, List[(String, JsValue)]] = nameToVal2_l.groupBy(_._1)
		val nameToVals_l: List[(String, List[JsValue])] = nameToVals_m.toList.map(pair => pair._1 -> pair._2.map(_._2))
		// If the nameToType list has "id" and we are passed id_?:
		val (nameToType2_l, map0): (List[(String, ru.Type)], Map[String, JsValue]) = (id_?, nameToType_l) match {
			case (Some(id), ("id", _) :: rest) => (rest, Map("id" -> JsString(id)))
			case _ => (nameToType_l, Map())
		}
		
		for {
			nameToVal3_l <- RsResult.toResultOfList(nameToVals_l.map(pair => {
				val (name, jsval_l) = pair
				jsval_l match {
					case jsval :: Nil => RsSuccess((name, jsval))
					case _ => RsError(s"too many values supplied for argument `$name`")
				}
			}))
			nameToVal_m = nameToVal3_l.toMap
			map <- doit(nameToType2_l, jsval_l, nameToVal_m, map0)
			//_ = println("map: "+map)
			res <- convMapString(path_r, JsObject(map), nameToType_l, eb, state_?, id_?)
		} yield res
	}
	
	private def parseStringToArgs(
		line: String
	): RsResult[List[(Option[String], JsValue)]] = {
		val arg_l = line.split(" ").toList
		val l = arg_l.map { s =>
			val i = s.indexOf("=")
			if (i > 0) (Some(s.substring(0, i)), JsString(s.substring(i + 1)))
			else (None, JsString(s))
		}
		RsSuccess(l)
	}

	def toJsValue(jsval: JsValue): RqResult[JsValue] =
		RqSuccess(jsval)
	
	def toString(jsval: JsValue): RqResult[String] = {
		jsval match {
			case JsString(text) => RqSuccess(text)
			case _ => RqError("expected JsString")
		}
	}
	
	def toInt(jsval: JsValue): RqResult[Int] = {
		jsval match {
			case JsNumber(n) => RqSuccess(n.toInt)
			case _ => RqError("expected JsNumber")
		}
	}
	
	def toInteger(jsval: JsValue): RqResult[Integer] = {
		jsval match {
			case JsNumber(n) => RqSuccess(n.toInt)
			case _ => RqError("expected JsNumber")
		}
	}
	
	def toDouble(jsval: JsValue): RqResult[Double] = {
		jsval match {
			case JsNumber(n) => RqSuccess(n.toDouble)
			case _ => RqError("expected JsNumber")
		}
	}
	
	def toBigDecimal(jsval: JsValue): RqResult[BigDecimal] = {
		jsval match {
			case JsNumber(n) => RqSuccess(n)
			case _ => RqError("expected JsNumber")
		}
	}
	
	def toBoolean(jsval: JsValue): RqResult[java.lang.Boolean] = {
		jsval match {
			case JsBoolean(b) => RqSuccess(b)
			case _ => RqError("expected JsBoolean")
		}
	}

	def toEnum[A <: Enumeration#Value : TypeTag](jsval: JsValue): RqResult[A] = {
		val typ = ru.typeTag[A].tpe
		toEnum(jsval, typ).map(_.asInstanceOf[A])
	}
	
	private def toEnum(jsval: JsValue, typ: ru.Type): RqResult[Any] = {
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
					value_l.find(_.toString == s).asRs(s"Value '$s' not valid for `${enumModule.name}`.  Expected one of ${value_l.mkString(", ")}.")
				case _ => RqError("expected JsString")
			}
		} catch {
			case e: Throwable => RqError(s"type: $typ, jsval: $jsval, error: ${e.getMessage}")
		}
	}
	
	private val RxVolume = """([0-9]*)(\.[0-9]*)? ?([mun]?l)""".r
	def toVolume(jsval: JsValue): RqResult[LiquidVolume] = {
		jsval match {
			case JsString(RxVolume(a,b,c)) =>
				val s = List(Option(a), Option(b)).flatten.mkString
				val n = BigDecimal(s)
				val v = c match {
					case "l" => LiquidVolume.l(n)
					case "ml" => LiquidVolume.ml(n)
					case "ul" => LiquidVolume.ul(n)
					case "nl" => LiquidVolume.nl(n)
					case _ => return RqError(s"invalid volume suffix '$c'")
				}
				RqSuccess(v)
			case JsNumber(n) => RqSuccess(LiquidVolume.l(n))
			case _ => RqError("expected JsString in volume format")
		}
	}

	def toPipetteSources(
		jsval: JsValue,
		eb: EntityBase,
		state_? : Option[WorldState]
	): RqResult[PipetteSources] = {
		state_? match {
			case None => RqError("require world state information for liquid source")
			case Some(state) =>
				jsval match {
					case JsString(s) =>
						val x = eb.lookupLiquidSources(s, state)
						//println("toPipetteSources: "+s+" -> "+x)
						x
					case JsArray(l) =>
						val l2 = RqResult.toResultOfList(l.map(jsval => toPipetteSources(jsval, eb, state_?)))
						l2.map(l => PipetteSources(l.flatMap(_.sources)))
					case _ => RqError("expected JsString in liquid source")
				}
		}
	}

	def toPipetteAmount(
		jsval: JsValue
	): RqResult[PipetteAmount] = {
		jsval match {
			case JsString(s) =>
				//println("parse: "+PipetteAmountParser.parse(s))
				PipetteAmountParser.parse(s)
			case _ => RqError("expected JsString for amount")
		}
	}

	def toPipetteDestinations(
		jsval: JsValue,
		eb: EntityBase,
		state_? : Option[WorldState]
	): RqResult[PipetteDestinations] = {
		state_? match {
			case None => RqError("require world state information for liquid source")
			case Some(state) =>
				jsval match {
					case JsString(s) =>
						eb.lookupLiquidDestinations(s, state)
					case JsArray(l) =>
						val l2 = RqResult.toResultOfList(l.map(jsval => toPipetteDestinations(jsval, eb, state_?)))
						l2.map(l => PipetteDestinations(l.flatMap(_.l)))
					case _ => RqError("expected JsString in liquid source")
				}
		}
	}
	
	def toEntityByRef[A <: Entity : Manifest](
		jsval: JsValue,
		eb: EntityBase
	): RqResult[A] = {
		jsval match {
			case JsString(s) => eb.getEntityAs[A](s)
			case _ => RqError("expected JsString as reference")
		}
	}
}