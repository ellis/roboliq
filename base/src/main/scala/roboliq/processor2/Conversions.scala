package roboliq.processor2

//import scala.language.existentials
//import scala.language.implicitConversions
//import scala.language.postfixOps
//import scalaz._
import scala.reflect.runtime.{universe => ru}
import spray.json._
import roboliq.core._
import RqPimper._


object ConversionsDirect {
	def toJsValue(jsval: JsValue): RqResult[JsValue] =
		RqSuccess(jsval)
	
	def toString(jsval: JsValue): RqResult[String] = {
		jsval match {
			case JsString(text) => RqSuccess(text)
			case _ => RqSuccess(jsval.toString)
		}
	}
	
	def toInteger(jsval: JsValue): RqResult[Integer] = {
		jsval match {
			case JsNumber(n) => RqSuccess(n.toInt)
			case _ => RqError("expected JsNumber")
		}
	}
	
	def toBoolean(jsval: JsValue): RqResult[java.lang.Boolean] = {
		jsval match {
			case JsBoolean(b) => RqSuccess(b)
			case _ => RqError("expected JsBoolean")
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
	
	def getWith[A](symbol: Symbol, jsobj: JsObject, fn: JsValue => RqResult[A]): RqResult[A] = {
		jsobj.fields.get(symbol.name).asRq(s"missing field `${symbol.name}`").flatMap(fn)
	}
	
	def getWith_?[A](symbol: Symbol, jsobj: JsObject, fn: JsValue => RqResult[A]): RqResult[Option[A]] = {
		jsobj.fields.get(symbol.name) match {
			case None => RqSuccess(None)
			case Some(jsval) => fn(jsval).map(Some(_))
		}
	}
	
	def getString(symbol: Symbol, jsobj: JsObject) = getWith(symbol, jsobj, toString)
	def getString_?(symbol: Symbol, jsobj: JsObject) = getWith_?(symbol, jsobj, toString)
	def getInteger(symbol: Symbol, jsobj: JsObject) = getWith(symbol, jsobj, toInteger)
	def getBoolean(symbol: Symbol, jsobj: JsObject) = getWith(symbol, jsobj, toBoolean)
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
	
	private def makeConversion(fn: JsValue => RqResult[Object]) = ConversionHandler1(
		(jsval: JsValue) =>
			fn(jsval).map(obj => List(ConversionItem_Object(obj)))
	)

	val asString = makeConversion(ConversionsDirect.toString)
	val asInteger = makeConversion(ConversionsDirect.toInteger)
	val asBoolean = makeConversion(ConversionsDirect.toBoolean)
	val asVolume = makeConversion(ConversionsDirect.toVolume)
	val asPlateModel = makeConversion(ConversionsDirect.toPlateModel)

	val asStringList = makeConversion(ConversionsDirect.toStringList)
	val asIntegerList = makeConversion(ConversionsDirect.toIntegerList)
	val asVolumeList = makeConversion(ConversionsDirect.toVolumeList)
	val asPlateModelList = makeConversion(ConversionsDirect.toPlateModelList)
	
	val plateLocationHandler = new ConversionHandlerN {
		val fnargs = fnRequire (
			as[String]('id), lookupPlateModel('plateModels), as[Boolean]('cooled)
		) { (id, plateModels, cooled) =>
			val loc = new PlateLocation(id, List(plateModels), cooled)
			returnObject(loc)
		}
	}
	
	val testHandler = new ConversionHandlerN {
		val fnargs = fnRequire (
			as[String]('id)
		) { (id) =>
			returnObject(Test(id))
		}
	}
	
	val plateHandler = ConversionHandler1(
		(jsval: JsValue) => {
			for {
				jsobj <- D.toJsObject(jsval)
				id <- D.getString('id, jsobj)
				idModel <- D.getString('idModel, jsobj)
				locationPermanent_? <- D.getString_?('locationPermanent, jsobj)
			} yield {
				List(RqItem_Function(RqFunctionArgs(
					arg_l = List(KeyClassOpt(KeyClass(TKP("plateModel", idModel, Nil), ru.typeOf[PlateModel]))),
					fn = (l: List[Object]) => InputListToTuple.check1[PlateModel](l).map { plateModel =>
						val plate = new Plate(id, plateModel, locationPermanent_?)
						List(ConversionItem_Object(plate))
					}
				)))
			}
		}
	)
	
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
