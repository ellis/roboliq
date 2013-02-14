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
	
	def conv(jsval: JsValue, typ: ru.Type): RqResult[Any] = {
		import scala.reflect.runtime.universe._
		import scala.reflect.runtime.{currentMirror => cm}

		println("conv: "+jsval+", "+typ)
		val ret = if (typ =:= typeOf[String]) {
			ConversionsDirect.toString(jsval)
		}
		else if (typ =:= typeOf[Int]) {
			jsval match {
				case JsNumber(v) => RqSuccess(v.toInt)
				case _ => RqError("expected JsNumber")
			}
		}
		else if (typ =:= typeOf[Integer]) {
			toInteger(jsval)
		}
		else if (typ <:< typeOf[List[Any]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
			jsval match {
				case JsArray(v) =>
					RqResult.toResultOfList(v.map(jsval => conv(jsval, typ2)))
				case _ => RqError("!")
			}
		}
		else if (typ <:< typeOf[LiquidVolume]) {
			toVolume(jsval)
		}
		else if (jsval.isInstanceOf[JsObject]) {
			val jsobj = jsval.asJsObject
			val ctor = typ.member(nme.CONSTRUCTOR).asMethod
			val p0_l = ctor.paramss(0)
			val p_l = p0_l.map(p => p.name.decoded -> p.typeSignature)
			for {
				arg_l <- RqResult.toResultOfList(p_l.map(pair => {
					val (name, typ2) = pair
					jsobj.fields.get(name) match {
						case Some(jsval2) => conv(jsval2, typ2)
						case None => RqError(s"missing field `$name`")
					}
				}))
			} yield {
				val c = typ.typeSymbol.asClass
				val mm = cm.reflectClass(c).reflectConstructor(ctor)
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
			'id.as[String], 'plateModels.lookupList[PlateModel], 'cooled.as[Boolean]
		) { (id, plateModels, cooled) =>
			// FIXME: plateModels needs to be a list
			val loc = new PlateLocation(id, plateModels, cooled)
			returnObject(loc)
		}
	}
	
	val testHandler = new ConversionHandlerN {
		val fnargs = fnRequire (
			'id.as[String]
		) { (id) =>
			returnObject(Test(id))
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
