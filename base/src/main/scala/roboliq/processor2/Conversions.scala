package roboliq.processor2

//import scala.language.existentials
import scala.language.implicitConversions
//import scala.language.postfixOps
//import scalaz._
import scala.reflect.runtime.{universe => ru}
//import scala.reflect.runtime.{currentMirror => cm}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf
import grizzled.slf4j.Logger
import spray.json._
import roboliq.core._
import RqPimper._


private case class TableInfo[A <: Object : TypeTag](
	table: String,
	id_? : Option[String] = None,
	toJson_? : Option[A => RqResult[JsValue]] = None,
	fromJson_? : Option[JsValue => RqResult[A]] = None
) {
	val typ = ru.typeTag[A].tpe
}

object ConversionsDirect {
	private val logger = Logger("roboliq.processor2.ConversionsDirect")
	
	private val tableInfo_l = List[TableInfo[_]](
		TableInfo[TipModel]("tipModel", None, None),
		TableInfo[PlateModel]("plateModel", None, None),
		TableInfo[TubeModel]("tubeModel", None, None),
		TableInfo[PlateLocation]("plateLocation", None, None),
		TableInfo[Tip]("tip", None, None),
		TableInfo[Substance]("substance", None, None),
		TableInfo[Plate]("plate", None, None),
		TableInfo[Vessel0]("vessel", None, None),
		TableInfo[TipState0]("tipState", Some("conf"), None, None),
		TableInfo[PlateState]("plateState", Some("plate"), None, None),
		TableInfo[VesselState]("vesselState", Some("vessel"), None, None),
		TableInfo[VesselSituatedState]("vesselSituatedState", Some("vesselState"), None, None)
	)
	
	private sealed trait ConvResult {
		def +(that: ConvResult): ConvResult
	}
	private case class ConvObject(o: Any) extends ConvResult {
		def +(that: ConvResult): ConvResult = {
			that
		}
	}
	
	private case class ConvRequire(require_m: Map[String, KeyClassOpt]) extends ConvResult {
		def +(that: ConvResult): ConvResult = {
			that match {
				case _: ConvObject => this
				case ConvRequire(m) => ConvRequire(require_m ++ m)
			}
		}
	}
	
	private def findTableInfoForType(tpe: ru.Type): RqResult[TableInfo[_]] = {
		tableInfo_l.find(tpe <:< _.typ).asRq(s"type `$tpe` has no table")
	}
	
	private def findTableInfoForTable(table: String): RqResult[TableInfo[_]] = {
		tableInfo_l.find(_.table == table).asRq(s"table `$table` has no table info")
	}
	
	def findTableForType(tpe: ru.Type): RqResult[String] = {
		findTableInfoForType(tpe).map(_.table)
	}
	
	def findIdFieldForTable(table: String): RqResult[String] = {
		findTableInfoForTable(table).map(_.id_?.getOrElse("id"))
	}
	
	/*
	def tableForType(tpe: ru.Type): String = {
		findTableForType(tpe) match {
			case Rq(table) => table
			case None =>
				val s = tpe.typeSymbol.name.decoded
				s.take(1).toLowerCase + s.tail
		}
	}
	*/
	
	def toJson[A: TypeTag](a: A): RqResult[JsValue] = {
		val typ = ru.typeTag[A].tpe
		toJson2(a, typ)
	}
	
	private def toJson2(obj: Any, typ: Type): RqResult[JsValue] = {
		logger.trace(s"toJson($obj, $typ)")
		if (typ <:< typeOf[String]) RqSuccess(JsString(obj.toString))
		else if (typ <:< typeOf[Int]) RqSuccess(JsNumber(obj.asInstanceOf[Int]))
		else if (typ <:< typeOf[Integer]) RqSuccess(JsNumber(obj.asInstanceOf[Integer]))
		else if (typ <:< typeOf[Boolean]) RqSuccess(JsBoolean(obj.asInstanceOf[Boolean]))
		else if (typ <:< typeOf[Float]) RqSuccess(JsNumber(obj.asInstanceOf[Float]))
		else if (typ <:< typeOf[Double]) RqSuccess(JsNumber(obj.asInstanceOf[Double]))
		else if (typ <:< typeOf[BigDecimal]) RqSuccess(JsNumber(obj.asInstanceOf[BigDecimal]))
		else if (typ <:< typeOf[LiquidVolume]) RqSuccess(JsString(obj.asInstanceOf[LiquidVolume].toString))
		else if (typ <:< typeOf[Enumeration#Value]) RqSuccess(JsString(obj.toString))
		else if (typ <:< typeOf[Option[_]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args(0)
			obj.asInstanceOf[Option[_]] match {
				case Some(x) => toJsonOrId(x, typ2)
				case None => RqSuccess(JsNull)
			}
		}
		else if (typ <:< typeOf[Map[_, _]]) {
			val typKey = typ.asInstanceOf[ru.TypeRefApi].args(0)
			val typVal = typ.asInstanceOf[ru.TypeRefApi].args(1)
			val l = obj.asInstanceOf[scala.collection.GenTraversable[(_, _)]].toList
			val key_l = findTableForType(typKey) match {
				case RqSuccess(_, _) =>
					val mirror = ru.runtimeMirror(this.getClass.getClassLoader)
					val clazz = mirror.runtimeClass(typKey.typeSymbol.asClass)
					// TODO: return a RqError if this method doesn't exist
					val method = clazz.getMethod("id")
					l.map(pair => {
						val (objKey, _) = pair
						val id = method.invoke(objKey).toString
						id
					})
				case _ =>
					l.map(_._1.toString)
			}
			val value_l = RqResult.toResultOfList(l.map(pair => toJsonOrId(pair._2, typVal)))
			RqResult.toResultOfTuple(RqSuccess(key_l), value_l).map(pair => {
				val (key_l, value_l) = pair
				val m = (key_l zip value_l).toMap
				JsObject(m)
			})
		}
		else if (typ <:< typeOf[scala.collection.GenTraversable[_]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args(0)
			val l = obj.asInstanceOf[scala.collection.GenTraversable[_]]
			RqResult.toResultOfList(l.map(o => toJson2(o, typ2)).toList).map(JsArray(_))
		}
		else if (typ <:< typeOf[Object]) {
			val ctor = typ.member(ru.nme.CONSTRUCTOR).asMethod
			val p0_l = ctor.paramss(0)
			val tableInfo_? = findTableInfoForType(typ)
			val idName = tableInfo_?.map(_.id_?.getOrElse("id")).getOrElse("id")
			val nameToType_l = p0_l.map(p => (p.name.encoded, p.name.decoded.replace("_?", "").replace(idName, "id"), p.typeSignature))
			val mirror = ru.runtimeMirror(this.getClass.getClassLoader)
			val clazz = mirror.runtimeClass(typ.typeSymbol.asClass)
			RqResult.toResultOfList(nameToType_l.map(pair => {
				val (name, label, typ2) = pair
				val method = clazz.getMethod(name)
				val obj2 = method.invoke(obj)
				toJsonOrId(obj2, typ2).map(label -> _)
			})).map(nameToValue_l => JsObject(nameToValue_l.toMap))
		}
		else {
			RqError(s"Unhandled type: ${typ}")
		}
		
	}

	private def toJsonOrId(obj: Any, typ: Type): RqResult[JsValue] = {
		try {
			findTableForType(typ) match {
				case RqSuccess(_, _) =>
					val mirror = ru.runtimeMirror(this.getClass.getClassLoader)
					val clazz = mirror.runtimeClass(typ.typeSymbol.asClass)
					// TODO: return a RqError if this method doesn't exist
					val method = clazz.getMethod("id")
					val id = method.invoke(obj).toString
					RqSuccess(JsString(id))
				case _ =>
					toJson2(obj, typ)
			}
		} catch {
			case e: Throwable => println(s"type: $typ, obj: $obj, error: ${e.getMessage}")
				RqError(s"type: $typ, obj: $obj, error: ${e.getMessage}")
		}
	}

	//private def reduceConvResultList(l: List[ConvResult]): 
	def conv(jsval: JsValue, typ: ru.Type, lookup_m: Map[String, Any] = Map()): RqResult[Any] = {
		convOrRequire(Nil, jsval, typ, Nil, Some(lookup_m)).flatMap(_ match {
			case ConvRequire(m) => RqError("need to lookup values for "+m.keys.mkString(", "))
			case ConvObject(o) => RqSuccess(o)
		})
	}

	def convRequirements(jsval: JsValue, typ: ru.Type, time: List[Int] = Nil): RqResult[Either[Map[String, KeyClassOpt], Any]] = {
		convOrRequire(Nil, jsval, typ, time, None).flatMap(_ match {
			case ConvRequire(m) => RqSuccess(Left(m))
			case ConvObject(o) => RqSuccess(Right(o))
		})
	}

	def convRequirements[A <: Object : TypeTag](jsval: JsValue, time: List[Int] = Nil): RqResult[Either[Map[String, KeyClassOpt], A]] = {
		convRequirements(jsval, ru.typeTag[A].tpe, time).map(_ match {
			case Left(x) => Left(x)
			case Right(o) => Right(o.asInstanceOf[A])
		})
	}
	
	private def convOrRequire(path_r: List[String], jsval: JsValue, typ: ru.Type, time: List[Int], lookup_m_? : Option[Map[String, Any]]): RqResult[ConvResult] = {
		import scala.reflect.runtime.universe._

		val mirror = runtimeMirror(this.getClass.getClassLoader)
		//val mirror = scala.reflect.runtime.currentMirror

		val path = path_r.reverse.mkString(".")
		val prefix = if (path_r.isEmpty) "" else path + ": "
		logger.trace(s"convOrRequire(${path}, $jsval, $typ, $time, ${}lookup_m_?})")

		try {
			jsval match {
				case JsString(s) =>
					// An initial "*" indicates a reference to another object by ID.
					val id_? = {
						if (s.startsWith("*"))
							Some(s.tail)
						else if (findTableForType(typ).isSuccess)
							Some(s)
						else
							None
					}
					if (id_?.isDefined) {
						return lookup_m_? match {
							// If we've been passed the map of objects to lookup:
							case Some(lookup_m) =>
								lookup_m.get(path) match {
									case Some(o) => RqSuccess(ConvObject(o))
									case None => RqError(s"value at `$path = $s` not found")
								}
							// Otherwise create a list of required objects
							case None =>
								findTableForType(typ).map { table =>
									val id = id_?.get
									val tkp = TKP(table, id, Nil)
									val kco = KeyClassOpt(KeyClass(tkp, typ, time), false, None)
									ConvRequire(Map(path -> kco))
								}
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
			
			val ret: RqResult[ConvResult] =
			if (typ =:= typeOf[String]) ConversionsDirect.toString(jsval)
			else if (typ =:= typeOf[Int]) toInt(jsval)
			else if (typ =:= typeOf[Integer]) toInteger(jsval)
			else if (typ =:= typeOf[Double]) toDouble(jsval)
			else if (typ =:= typeOf[BigDecimal]) toBigDecimal(jsval)
			else if (typ =:= typeOf[Boolean]) toBoolean(jsval)//.map(_.asInstanceOf[Boolean])
			else if (typ =:= typeOf[java.lang.Boolean]) toBoolean(jsval)
			else if (typ <:< typeOf[Enumeration#Value]) toEnum(jsval, typ)
			else if (typ =:= typeOf[LiquidVolume]) toVolume(jsval)
			else if (typ <:< typeOf[Substance]) toSubstance(jsval)
			else if (typ <:< typeOf[Option[_]]) {
				val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
				if (jsval == JsNull) RqSuccess(ConvObject(None))
				else convOrRequire(path_r, jsval, typ2, time, lookup_m_?).map(_ match {
					case ConvObject(o) => ConvObject(Option(o))
					case res => res
				})
			}
			else if (typ <:< typeOf[List[_]]) {
				val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
				convList(path_r, jsval, typ2, time, lookup_m_?)
			}
			else if (typ <:< typeOf[Set[_]]) {
				val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
				convList(path_r, jsval, typ2, time, lookup_m_?).map(_ match {
					case ConvObject(l: List[_]) => ConvObject(Set(l : _*))
					case r => r
				})
			}
			else if (typ <:< typeOf[Map[_, _]]) {
				jsval match {
					case jsobj @ JsObject(fields) =>
						val typKey = typ.asInstanceOf[ru.TypeRefApi].args(0)
						val typVal = typ.asInstanceOf[ru.TypeRefApi].args(1)
						val name_l = fields.toList.map(_._1)
						val nameToType_l = name_l.map(_ -> typVal)
						convMap(path_r, jsobj, typKey, nameToType_l, time, lookup_m_?)
					case JsNull => RqSuccess(ConvObject(Map()))
					case _ =>
						RqError("expected a JsObject")
				}
			}
			else if (jsval.isInstanceOf[JsObject]) {
				val jsobj = jsval.asJsObject
				val ctor = typ.member(nme.CONSTRUCTOR).asMethod
				val p0_l = ctor.paramss(0)
				val nameToType_l = p0_l.map(p => p.name.decoded.replace("_?", "") -> p.typeSignature)
				convMap(path_r, jsobj, typeOf[String], nameToType_l, time, lookup_m_?).map(_ match {
					case ConvObject(o) =>
						val nameToObj_m = o.asInstanceOf[Map[String, _]]
						val arg_l = nameToType_l.map(pair => nameToObj_m(pair._1))
						val c = typ.typeSymbol.asClass
						val mm = mirror.reflectClass(c).reflectConstructor(ctor)
						logger.debug("arg_l: "+arg_l)
						val obj = mm(arg_l : _*)
						ConvObject(obj)
					case r => r
				})
			}
			else {
				RqError(s"unhandled type or value. type=${typ}, value=${jsval}")
			}
			logger.debug(ret)
			ret
		}
		catch {
			case e: Throwable => //e.RqError(s"error converting `$path`: "+e.getStackTrace())
				throw e
		}
	}
	
	private def convList(path_r: List[String], jsval: JsValue, typ2: ru.Type, time: List[Int], lookup_m_? : Option[Map[String, Any]]): RqResult[ConvResult] = {
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
					convOrRequire(path2_r, jsval2, typ2, time, lookup_m_?)
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
				RqSuccess(ConvObject(Nil))
			case _ =>
				convOrRequire(path_r, jsval, typ2, time, lookup_m_?).map(_ match {
					case x: ConvRequire => x
					case ConvObject(o) => ConvObject(List(o))
				}).orElse(RqError(s"expected an array of ${typ2.typeSymbol.name.toString}"))
		}
	}
	
	private def convMap(path_r: List[String], jsobj: JsObject, typKey: Type, nameToType_l: List[(String, ru.Type)], time: List[Int], lookup_m_? : Option[Map[String, Any]]): RqResult[ConvResult] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		// Handle keys if they need to be looked up
		val (errK_l, wK, convK_l, key_l):
			(List[String], List[String], Map[String, KeyClassOpt], List[_]) =
			findTableForType(typKey) match {
				case RqSuccess(_, _) =>
					val res0 = RqResult.toResultOfList(nameToType_l.map(pair => {
						val (id, _) = pair
						val path2_r = (id + "#") :: path_r
						convOrRequire(path2_r, JsString(id), typKey, time, lookup_m_?)
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
					case Some(jsval2) => convOrRequire(path2_r, jsval2, typ2, time, lookup_m_?)
					case None =>
						// Field is missing
						// If this should be an object in the database and an `id` field is available,
						if (findTableForType(typ2).isSuccess && jsobj.fields.contains("id"))
							convOrRequire(path2_r, jsobj.fields("id"), typ2, time, lookup_m_?)
						// Else try using JsNull
						else
							convOrRequire(path2_r, JsNull, typ2, time, lookup_m_?)
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
					RqSuccess(ConvObject((key_l zip val_l).toMap), warning_l)
				}
				else {
					RqSuccess(ConvRequire(conv_l), warning_l)
				}
			case _ =>
				RqError(err_l, warning_l)
		}
		/*
		val convK_l: List[ConvRequire] = resKey_?.map(_.collect{case o: ConvRequire => o})
		val convV_l: List[ConvRequire] = resVal.map(_.collect{case o: ConvRequire => o})
		
		val res0 = (resKey_?, resVal) match {
			case (None, _) => resVal
			case (Some(RqSuccess(l1, w1)), RqSuccess(l2, w2)) => RqSuccess(l1 ++ l2, w1 ++ w2)
			case (Some(r), _) => r.flatMap(_ => resVal)
		}
		
		// If there were no errors in conversion,
		res0.map(l => {
			// If there are any requirements, return a list of all requirements.
			if (l.exists(_.isInstanceOf[ConvRequire])) {
				ConvRequire(l.collect({case ConvRequire(m) => m}).flatten.toMap)
			}
			// Otherwise, return map of names to objects.
			else {
				ConvObject((nameToType_l zip l).collect({case ((name: String, _), ConvObject(o)) => name -> o}).toMap)
			}
		})*/
	}
	
	private 

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
					value_l.find(_.toString == s).asRq(s"Value '$s' not valid for `${enumModule.name}`.  Expected one of ${value_l.mkString(", ")}.")
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
	
	def toJsObject(jsval: JsValue): RqResult[JsObject] =
		jsval match {
			case jsobj: JsObject => RqSuccess(jsobj)
			case _ => RqError("required a JsObject, but have "+jsval.getClass+": "+jsval)
		}
	
	/*def toTipModel(jsval: JsValue): RqResult[TipModel] = {
		for {
			jsobj <- toJsObject(jsval)
			id <- getString('id, jsobj)
			volume <- getVolume('volume, jsobj).orElse(RqSuccess(LiquidVolume.empty))
			volumeMin <- getVolume('volumeMin, jsobj).orElse(RqSuccess(LiquidVolume.empty))
		} yield {
			new TipModel(id, volume, volumeMin, LiquidVolume.empty, LiquidVolume.empty)
		}
	}*/
	
	def toPlateModel(jsval: JsValue): RqResult[PlateModel] = {
		for {
			jsobj <- toJsObject(jsval)
			id <- getString('id, jsobj)
			rows <- getInteger('rows, jsobj)
			cols <- getInteger('cols, jsobj)
			wellVolume <- getVolume('wellVolume, jsobj)
		} yield {
			new PlateModel(id, rows, cols, wellVolume)
		}
	}
	
	def toSubstance(jsval: JsValue): RqResult[Substance] = {
		for {
			jsobj <- toJsObject(jsval)
			id <- getString('id, jsobj)
			kind <- getString('kind, jsobj)
			costPerUnit_? <- getBigDecimal_?('costPerUnit, jsobj)
			substance <- kind match {
				case "liquid" =>
					for {
						physicalProperties <- 'physicalProperties.as[LiquidPhysicalProperties.Value](jsobj) 
						cleanPolicy <- 'cleanPolicy.as[TipCleanPolicy](jsobj)
					} yield {
						SubstanceLiquid(id, physicalProperties, cleanPolicy, costPerUnit_?)
					}
				case "dna" =>
					for {
						sequence_? <- getString_?('sequence, jsobj)
					} yield {
						SubstanceDna(id, sequence_?, costPerUnit_?)
					}
				case "solid" =>
					RqSuccess(SubstanceOther(id, costPerUnit_?))
				case _ => RqError("unknown value for `kind`")
			}
		} yield substance
	}
	
	def getWith[A](symbol: Symbol, jsobj: JsObject, fn: JsValue => RqResult[A]): RqResult[A] = {
		jsobj.fields.get(symbol.name).asRq(s"missing field `${symbol.name}`").flatMap(fn)
	}
	
	def getWith_?[A](symbol: Symbol, jsobj: JsObject, fn: JsValue => RqResult[A]): RqResult[Option[A]] = {
		jsobj.fields.get(symbol.name) match {
			case None => RqSuccess(None)
			case Some(jsval) => fn(jsval).map(Some(_))
		}
	}

	implicit class SymbolWrapper(symbol: Symbol) {
		private def conv2[A: TypeTag](jsval: JsValue) = conv(jsval, ru.typeTag[A].tpe).map(_.asInstanceOf[A])
		def as[A: TypeTag](jsobj: JsObject): RqResult[A] = getWith(symbol, jsobj, conv2[A] _)
	}
	
	def getString(symbol: Symbol, jsobj: JsObject) = getWith(symbol, jsobj, toString)
	def getString_?(symbol: Symbol, jsobj: JsObject) = getWith_?(symbol, jsobj, toString)
	def getInteger(symbol: Symbol, jsobj: JsObject) = getWith(symbol, jsobj, toInteger)
	def getBigDecimal(symbol: Symbol, jsobj: JsObject) = getWith(symbol, jsobj, toBigDecimal)
	def getBigDecimal_?(symbol: Symbol, jsobj: JsObject) = getWith_?(symbol, jsobj, toBigDecimal)
	def getBoolean(symbol: Symbol, jsobj: JsObject) = getWith(symbol, jsobj, toBoolean)
	def getEnum[A <: Enumeration#Value : TypeTag](symbol: Symbol, jsobj: JsObject) = getWith(symbol, jsobj, toEnum[A])
	def getVolume(symbol: Symbol, jsobj: JsObject) = getWith(symbol, jsobj, toVolume)
	def getStringList(symbol: Symbol, jsobj: JsObject) = getWith(symbol, jsobj, toStringList)

	
	private def toList[A](jsval: JsValue, fn: JsValue => RqResult[A]): RqResult[List[A]] = {
		jsval match {
			case JsArray(elements) => RqResult.toResultOfList(elements.map(fn))
			case _ =>
				fn(jsval) match {
					case RqSuccess(a, w) => RqSuccess(List(a), w)
					case r => RqError("expected JsArray of JsStrings")
				}
		}
	}
	
	def toStringList(jsval: JsValue) = toList(jsval, toString _)
	def toIntegerList(jsval: JsValue) = toList(jsval, toInteger _)
	def toVolumeList(jsval: JsValue) = toList(jsval, toVolume _)
	def toPlateModelList(jsval: JsValue) = toList(jsval, toPlateModel _)
}

object Conversions {
	private val logger = Logger("roboliq.processor2.Conversions")
	
	private val D = ConversionsDirect

	def readByIdAt[A <: Object : TypeTag](db: DataBase, id: String, time: List[Int]): RqResult[A] = {
		val typ = ru.typeTag[A].tpe
		logger.trace(s"readByIdAt[$typ](db, $id, $time)")
		for {
			table <- ConversionsDirect.findTableForType(typ)
			kc = KeyClass(TKP(table, id, Nil), typ, time)
			ret <- readAnyAt(db, kc).map(_.asInstanceOf[A])
		} yield ret
	}

	def readByIdBefore[A <: Object : TypeTag](db: DataBase, id: String, time: List[Int]) = {
		val typ = ru.typeTag[A].tpe
		logger.trace(s"readByIdBefore[$typ](db, $id, $time)")
		for {
			table <- ConversionsDirect.findTableForType(typ)
			kc = KeyClass(TKP(table, id, Nil), typ, time)
			ret <- readAnyBefore(db, kc).map(_.asInstanceOf[A])
		} yield ret
	}
	
	def readAnyAt(db: DataBase, kc: KeyClass): RqResult[Any] = {
		logger.trace(s"readAnyAt(db, $kc)")
		for {
			jsval <- db.getAt(kc.key, kc.time)
			either <- D.convRequirements(jsval, kc.clazz, kc.time)
			ret <- either match {
				case Right(ret) => RqSuccess(ret)
				case Left(require_m) => 
					for {
						lookup_l <- RqResult.toResultOfList(require_m.toList.map(pair => {
							val (name, kco) = pair
							for {
								ret <- readAnyAt(db, kco.kc)
							} yield name -> ret
						}))
						lookup_m = lookup_l.toMap
						ret <- D.conv(jsval, kc.clazz, lookup_m)
					} yield ret
			}
		} yield ret
	}
	
	def readAnyBefore(db: DataBase, kc: KeyClass): RqResult[Any] = {
		for {
			jsval <- db.getBefore(kc.key, kc.time)
			either <- D.convRequirements(jsval, kc.clazz)
			ret <- either match {
				case Right(ret) => RqSuccess(ret)
				case Left(require_m) => 
					for {
						lookup_l <- RqResult.toResultOfList(require_m.toList.map(pair => {
							val (name, kco) = pair
							for {
								ret <- readAnyBefore(db, kco.kc)
							} yield name -> ret
						}))
						lookup_m = lookup_l.toMap
						ret <- D.conv(jsval, kc.clazz, lookup_m)
					} yield ret
			}
		} yield ret
	}
	
	private def makeConversion(fn: JsValue => RqResult[Object]) = ConversionHandler1(
		(jsval: JsValue) =>
			fn(jsval).map(obj => List(ConversionItem_Object(obj)))
	)

	/*val tipHandler = new ConversionHandlerN {
		val fnargs = fnRequire (
			'index.as[Integer], 'model.lookup_?[TipModel]
		) { (index, modelPermanent_?) =>
			returnObject(new Tip(index, modelPermanent_?))
		}
	}
	
	val plateLocationHandler = new ConversionHandlerN {
		val fnargs = fnRequire (
			'id.as[String], 'plateModels.lookupList[PlateModel], 'cooled.as[Boolean]
		) { (id, plateModels, cooled) =>
			val loc = new PlateLocation(id, plateModels, cooled)
			returnObject(loc)
		}
	}
	
	val plateHandler = new ConversionHandlerN {
		val fnargs = fnRequire (
			'id.as[String],
			'idModel.lookup[PlateModel],
			'locationPermanent.as[Option[String]]
		) { (id, plateModel, locationPermanent_?) =>
			returnObject(new Plate(id, plateModel, locationPermanent_?))
		}
	}*/
	
	/*val plateStateHandler = new ConversionHandlerN {
		val fnargs = fnRequire (
			'id.lookup[Plate],
			'location.lookup_?[PlateLocation]
		) { (plate, location_?) =>
			val plateState = new PlateState(plate, location_?)
			returnObject(plateState)
		}
	}*/
	
	def tipCleanPolicyToJson(policy: TipCleanPolicy): JsValue = {
		JsObject(Map(
			"enter" -> JsString(policy.enter.toString),
			"exit" -> JsString(policy.exit.toString)
		))
	}
	
	def liquidToJson(liquid: Liquid): JsValue = {
		import liquid._
		JsObject(Map(
			"id" -> JsString(id),
			"sName" -> sName_?.map(JsString(_)).getOrElse(JsNull),
			"sFamily" -> JsString(sFamily),
			"contaminants" -> JsArray(contaminants.toList.map(v => JsString(v.toString))),
			"tipCleanPolicy" -> tipCleanPolicyToJson(tipCleanPolicy),
			"multipipetteThreshold" -> JsNumber(multipipetteThreshold)
		))
	}
	
	def tipStateToJson(tipState: TipState0): JsValue = {
		import tipState._
		JsObject(Map(
			"id" -> JsString(conf.id),
			"model" -> model_?.map(v => JsString(v.id)).getOrElse(JsNull),
			"src" -> src_?.map(v => JsString(v.vessel.id)).getOrElse(JsNull),
			"liquid" -> liquidToJson(liquid),
			"volume" -> JsString(volume.toString),
			"contamInside" -> JsArray(contamInside.toList.map(v => JsString(v.toString))),
			"nContamInsideVolume" -> JsString(nContamInsideVolume.toString),
			"contamOutside" -> JsArray(contamOutside.toList.map(v => JsString(v.toString))),
			"srcsEntered" -> JsArray(srcsEntered.toList.map(liquidToJson)),
			"destsEntered" -> JsArray(destsEntered.toList.map(liquidToJson)),
			"cleanDegree" -> JsString(cleanDegree.toString),
			"cleanDegreePrev" -> JsString(cleanDegreePrev.toString),
			"cleanDegreePending" -> JsString(cleanDegreePending.toString)
		))
	}
	
	def vesselContentToJson(vesselContent: VesselContent): JsValue = {
		JsObject(
			"solventToVolume" -> JsObject(vesselContent.solventToVolume.toList.map(pair => pair._1.id -> JsString(pair._2.toString)).toMap),
			"soluteToMol" -> JsObject(vesselContent.soluteToMol.toList.map(pair => pair._1.id -> JsNumber(pair._2)).toMap)
		)
	}
	
	def vesselStateToJson(vesselState: VesselState): JsValue = {
		JsObject(
			"id" -> JsString(vesselState.id),
			"content" -> vesselContentToJson(vesselState.content)
		)
	}
}
