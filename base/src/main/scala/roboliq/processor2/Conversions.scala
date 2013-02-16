package roboliq.processor2

//import scala.language.existentials
//import scala.language.implicitConversions
//import scala.language.postfixOps
//import scalaz._
import scala.reflect.runtime.{universe => ru}
//import scala.reflect.runtime.{currentMirror => cm}
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf
import spray.json._
import roboliq.core._
import RqPimper._


object ConversionsDirect {
	
	def conv(jsval: JsValue, typ: ru.Type, lookup_l: List[Object] = Nil): RqResult[Any] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		println("conv: "+jsval+", "+typ)
		// TODO: handle this by inspection somehow, so that the individual conversions
		// don't need to be maintained here.
		val ret =
		if (typ =:= typeOf[String]) ConversionsDirect.toString(jsval)
		else if (typ =:= typeOf[Int]) toInt(jsval)
		else if (typ =:= typeOf[Integer]) toInteger(jsval)
		else if (typ <:< typeOf[Enumeration#Value]) toEnum(jsval, typ)
		else if (typ <:< typeOf[Option[Any]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
			conv(jsval, typ2, lookup_l).map(Option.apply)
		}
		else if (typ <:< typeOf[List[Any]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
			jsval match {
				case JsArray(v) =>
					RqResult.toResultOfList(v.map(jsval => conv(jsval, typ2, lookup_l)))
				case _ => RqError("!")
			}
		}
		else if (typ <:< typeOf[LiquidVolume]) toVolume(jsval)
		else if (typ <:< typeOf[PipettePosition.Value]) toPipettePosition(jsval)
		else if (typ <:< typeOf[PipettePolicy]) toPipettePolicy(jsval)
		else if (jsval.isInstanceOf[JsObject]) {
			val jsobj = jsval.asJsObject
			val ctor = typ.member(nme.CONSTRUCTOR).asMethod
			val p0_l = ctor.paramss(0)
			val p_l = p0_l.map(p => p.name.decoded -> p.typeSignature)
			var lookup2_l = lookup_l
			for {
				arg_l <- RqResult.toResultOfList(p_l.map(pair => {
					val (name0, typ2) = pair
					val name = name0.replace("_?", "")
					jsobj.fields.get(name) match {
						case Some(jsval2) => conv(jsval2, typ2, lookup2_l)
						case None =>
							jsobj.fields.get("&"+name) match {
								case Some(JsString(id)) =>
									lookup2_l match {
										case lookup :: rest =>
											lookup2_l = rest
											RqSuccess(lookup)
										case _ =>
											RqError(s"No value for `$name` in lookup list")
									}
								case Some(jsval2) =>
									RqError(s"Require string for ID")
								case None =>
									if (typ2 <:< typeOf[Option[Any]]) {
										RqSuccess(None)
									}
									else if (typ2 <:< typeOf[List[Any]]) {
										RqSuccess(Nil)
									}
									else {
										RqError(s"missing field `$name`")
									}
							}
					}
				}))
			} yield {
				val c = typ.typeSymbol.asClass
				val mm = mirror.reflectClass(c).reflectConstructor(ctor)
				mm(arg_l : _*)
			}
		}
		else {
			RqError("Unhandled type")
		}
		println(ret)
		ret
	}

	def toJsValue(jsval: JsValue): RqResult[JsValue] =
		RqSuccess(jsval)
	
	def toString(jsval: JsValue): RqResult[String] = {
		jsval match {
			case JsString(text) => RqSuccess(text)
			case _ => RqSuccess(jsval.toString)
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
			case _ => RqError("expected JsNumber")
		}

	}
	
	private val RxVolume = """([0-9]*)(\.[0-9]*)?([mun]?l)""".r
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
	
	def toTipModel(jsval: JsValue): RqResult[TipModel] = {
		for {
			jsobj <- toJsObject(jsval)
			id <- getString('id, jsobj)
			volume <- getVolume('volume, jsobj).orElse(RqSuccess(LiquidVolume.empty))
			volumeMin <- getVolume('volumeMin, jsobj).orElse(RqSuccess(LiquidVolume.empty))
		} yield {
			new TipModel(id, volume, volumeMin, LiquidVolume.empty, LiquidVolume.empty)
		}
	}
	
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
						cleanPolicy <- 'cleanPolicy.as[GroupCleanPolicy](jsobj)
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
	
	def toPipettePosition(jsval: JsValue): RqResult[PipettePosition.Value] = {
		for {
			s <- toString(jsval)
		} yield {
			PipettePosition.withName(s)
		}
	}
	
	def toPipettePolicy(jsval: JsValue): RqResult[PipettePolicy] = {
		jsval match {
			case JsString(name) => RqSuccess(PipettePolicy.fromName(name))
			case _ =>
				for {
					jsobj <- toJsObject(jsval)
					id <- getString('id, jsobj)
					pos <- getWith('pos, jsobj, toPipettePosition _)
				} yield {
					PipettePolicy(id, pos)
				}
		}
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
		private def conv2[A: TypeTag](jsval: JsValue) = conv(jsval, ru.typeTag[A].tpe, Nil).map(_.asInstanceOf[A])
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
	private val D = ConversionsDirect
	
	def tableForType(tpe: ru.Type): String = {
		val s = tpe.typeSymbol.name.decoded
		s.take(1).toLowerCase + s.tail
	}

	def convLookup(jsval: JsValue, typ: ru.Type): RqResult[List[KeyClassOpt]] = {
		import scala.reflect.runtime.universe._
		import scala.reflect.runtime.{currentMirror => cm}

		println("convLookup: "+jsval+", "+typ)
		val ret =
		if (typ <:< typeOf[List[Any]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
			jsval match {
				case JsArray(v) =>
					RqResult.toResultOfList(v.map(jsval => convLookup(jsval, typ2))).map(_.flatten)
				case _ => convLookup(jsval, typ2)
			}
		}
		else if (jsval.isInstanceOf[JsObject]) {
			val jsobj = jsval.asJsObject
			val ctor = typ.member(nme.CONSTRUCTOR).asMethod
			val p0_l = ctor.paramss(0)
			val p_l = p0_l.map(p => p.name.decoded -> p.typeSignature)
			RqResult.toResultOfList(p_l.map(pair => {
				val (name, typ2) = pair
				jsobj.fields.get(name) match {
					case Some(jsval2) => convLookup(jsval2, typ2)
					case None =>
						jsobj.fields.get("&"+name) match {
							case Some(JsString(id)) =>
								val table = tableForType(typ2)
								val tkp = TKP(table, id, Nil)
								RqSuccess(List(KeyClassOpt(KeyClass(tkp, typ2), false, None)))
							case Some(jsval2) =>
								RqError(s"Require string for ID")
							case None =>
								if (typ2 <:< typeOf[Option[Any]]) {
									RqSuccess(Nil)
								}
								else if (typ2 <:< typeOf[List[Any]]) {
									RqSuccess(Nil)
								}
								else {
									RqError(s"missing field `$name`")
								}
						}
				}
			})).map(_.flatten)
		}
		else {
			RqSuccess(Nil)
		}
		println(ret)
		ret
	}

	private def makeConversion(fn: JsValue => RqResult[Object]) = ConversionHandler1(
		(jsval: JsValue) =>
			fn(jsval).map(obj => List(ConversionItem_Object(obj)))
	)

	val asString = makeConversion(ConversionsDirect.toString)
	val asInteger = makeConversion(ConversionsDirect.toInteger)
	val asBoolean = makeConversion(ConversionsDirect.toBoolean)
	val asVolume = makeConversion(ConversionsDirect.toVolume)
	val tipModelHandler = makeConversion(ConversionsDirect.toTipModel)
	val asPlateModel = makeConversion(ConversionsDirect.toPlateModel)

	val asStringList = makeConversion(ConversionsDirect.toStringList)
	val asIntegerList = makeConversion(ConversionsDirect.toIntegerList)
	val asVolumeList = makeConversion(ConversionsDirect.toVolumeList)
	val asPlateModelList = makeConversion(ConversionsDirect.toPlateModelList)
	
	val tipHandler = new ConversionHandlerN {
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
	}
	
	val plateStateHandler = new ConversionHandlerN {
		val fnargs = fnRequire (
			'id.lookup[Plate],
			'location.lookup_?[PlateLocation]
		) { (plate, location_?) =>
			val plateState = new PlateState(plate, location_?)
			returnObject(plateState)
		}
	}
}
