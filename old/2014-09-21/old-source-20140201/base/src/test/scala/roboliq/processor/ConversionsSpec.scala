package roboliq.processor

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import grizzled.slf4j.Logger
import org.scalatest.FunSpec
import spray.json._
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import ConversionsDirect._
import roboliq.test.Config01


private object A extends Enumeration {
	val B, C = Value
}

private case class MyClass1(s: String, n: Int, l: List[Boolean])

class ConversionsSpec extends FunSpec {
	private val logger = Logger[this.type]
	
	private def getTypeTag[T: TypeTag](obj: T) = ru.typeTag[T]
	private def getType[T: TypeTag](obj: T) = ru.typeTag[T].tpe
	
	import Config01.water
	val powder = Substance.other("powder", TipCleanPolicy.DD, Set("DNA"))
	
	val liquid_water = Liquid(Map(water -> 1))

	describe("toJson") {
		def check[A: TypeTag](l: (A, JsValue)*) = {
			val typ = ru.typeTag[A].tpe
			it(s"should parse $typ") {
				for ((a, jsval) <- l) {
					assert(toJson(a) === RqSuccess(jsval))
				}
			}
		}
	
		check("Hello, World" -> JsString("Hello, World"), "" -> JsString(""))
		check(
				0 -> JsNumber(0),
				42 -> JsNumber(42))
		check(
				0.0 -> JsNumber(0.0),
				42.0 -> JsNumber(42.0))
		check(
				LiquidVolume.ul(42) -> JsString("42ul")
		)
		check(
				List(1, 2, 3) -> JsArray(JsNumber(1), JsNumber(2), JsNumber(3)),
				Nil -> JsArray(Nil)
		)
		check[Map[String, Int]](
				Map("a" -> 1, "b" -> 2) -> JsObject("a" -> JsNumber(1), "b" -> JsNumber(2)),
				Map() -> JsObject()
		)
		check(
				MyClass1("text", 42, List(true, false)) -> JsObject("s" -> JsString("text"), "n" -> JsNumber(42), "l" -> JsArray(JsBoolean(true), JsBoolean(false)))
		)

		val tipModel1000 = TipModel("Standard 1000ul", LiquidVolume.ul(950), LiquidVolume.ul(4))
		val tip = Tip("TIP1", "LiHa", 0, 0, 0, Some(tipModel1000))
		check(
			tip -> JsObject(
				"id" -> JsString("TIP1"),
				"deviceId" -> JsString("LiHa"),
				"index" -> JsNumber(0),
				"row" -> JsNumber(0),
				"col" -> JsNumber(0),
				"permanent" -> JsString("Standard 1000ul")
			)
		)
	}

	describe("conv") {
		def check[A: TypeTag](succeed_l: List[(JsValue, A)], fail_l: List[JsValue]) = {
			val typ = ru.typeTag[A].tpe
			it(s"should parse $typ") {
				for (pair <- succeed_l) {
					assert(conv(pair._1, typ) === RqSuccess(pair._2))
				}
				for (jsval <- fail_l) {
					assert(conv(jsval, typ).isError)
				}
			}
		}
	
		check[String](
			List(JsString("test") -> "test"),
			List(JsNumber(1), JsNull)
		)
		check[Int](
			List(JsNumber(42) -> 42),
			List(JsString("42"), JsNull)
		)
		check[Integer](
			List(JsNumber(42) -> 42),
			List(JsString("42"), JsNull)
		)
		check[BigDecimal](
			List(JsNumber(42) -> 42),
			List(JsString("42"), JsNull)
		)
		check[Boolean](
			List(JsBoolean(true) -> true, JsBoolean(false) -> false),
			List(JsString("42"), JsNumber(1), JsNumber(0), JsNull)
		)
		check[java.lang.Boolean](
			List(JsBoolean(true) -> true, JsBoolean(false) -> false),
			List(JsString("42"), JsNumber(1), JsNumber(0), JsNull)
		)
		check[A.Value](
			List(JsString("B") -> A.B, JsString("C") -> A.C),
			List(JsString(""), JsNumber(1), JsNull)
		)
		check[Option[String]](
			List(JsString("Hello") -> Some("Hello"), JsNull -> None),
			List(JsNumber(1))
		)
		check[List[Int]](
			List(
				JsArray(List(JsNumber(1), JsNumber(2), JsNumber(3))) -> List(1, 2, 3),
				JsArray(List()) -> Nil,
				JsNull -> Nil,
				JsNumber(1) -> List(1)),
			List(JsString("1"))
		)
		check[Set[Int]](
			List(
				JsArray(List(JsNumber(1), JsNumber(2), JsNumber(3))) -> Set(1, 2, 3),
				JsArray(List(JsNumber(1), JsNumber(2), JsNumber(2))) -> Set(1, 2),
				JsArray(List()) -> Set(),
				JsNull -> Set(),
				JsNumber(1) -> Set(1)),
			List(JsString("1"))
		)
		check[Map[String, Int]](
			List(
				JsonParser("""{"a": 1, "b": 2}""") -> Map("a" -> 1, "b" -> 2),
				JsNull -> Map()
			),
			List(JsString("1"))
		)
		check[PipettePolicy](
			List(
				JsonParser("""{"id": "myId", "pos": "WetContact"}""") -> PipettePolicy("myId", PipettePosition.WetContact)
			),
			List(JsNull)
		)
		check[Substance](
			List(
				JsonParser("""{"id": "water", "kind": "Liquid", "tipCleanPolicy": "ThoroughNone", "molarity": 55, "gramPerMole": 18}""") -> water
			),
			List(JsNull)
		)
		check[TipCleanPolicy](
			List(
				JsString("ThoroughNone") -> TipCleanPolicy.ThoroughNone
			),
			List(JsNull, JsString(""), JsString("x"))
		)
		/*check[VesselContent](
			List(
				JsonParser("""{"liquid": {"id": "<EMPTY>"}, "totalMole": 0}""") -> VesselContent.Empty
			),
			List(JsNull)
		)*/
	}
	
	describe("convRequirements") {
		it("should parse Map[Substance, Int]") {
			assert(
				convRequirements(JsonParser("""{"water": 1, "powder": 20}"""), typeOf[Map[Substance, Int]])
					=== RqSuccess(Left(Map(
						"water#" -> KeyClassOpt(KeyClass(TKP("substance", "water", Nil), typeOf[Substance]), false),
						"powder#" -> KeyClassOpt(KeyClass(TKP("substance", "powder", Nil), typeOf[Substance]), false)
					)))
			)
			assert(
				conv(
					JsonParser("""{"water": 1, "powder": 20}"""),
					typeOf[Map[Substance, Int]],
					Map(
						"water#" -> water,
						"powder#" -> powder
					))
					=== RqSuccess(Map(water -> 1, powder -> 20))
			)
		}
		it("should parse Map[String, Substance]") {
			assert(
				convRequirements(JsonParser("""{"first": "water", "second": "powder"}"""), typeOf[Map[String, Substance]])
					=== RqSuccess(Left(Map(
						"first" -> KeyClassOpt(KeyClass(TKP("substance", "water", Nil), typeOf[Substance]), false),
						"second" -> KeyClassOpt(KeyClass(TKP("substance", "powder", Nil), typeOf[Substance]), false)
					)))
			)
			assert(
				conv(
					JsonParser("""{"first": "water", "second": "powder"}"""),
					typeOf[Map[String, Substance]],
					Map(
						"first" -> water,
						"second" -> powder
					))
					=== RqSuccess(Map("first" -> water, "second" -> powder))
			)
		}
		it("should parse Liquid") {
			assert(
				conv(
					JsonParser("""{}"""),
					typeOf[Liquid],
					Map())
					=== RqSuccess(Liquid.Empty)
			)
			assert(
				conv(
					JsonParser("""{ "water": 1 }"""),
					typeOf[Liquid],
					Map(
						"water#" -> water
					))
					=== RqSuccess(liquid_water)
			)
		}
		it("should parse VesselContent") {
			assert(
				conv(
					JsonParser("""{}"""),
					typeOf[VesselContent],
					Map())
					=== RqSuccess(VesselContent.Empty)
			)
			assert(
				conv(
					JsonParser("""{ "water": 55 }"""),
					typeOf[VesselContent],
					Map(
						"water#" -> water
					))
					=== VesselContent.fromVolume(water, LiquidVolume.l(1))
			)
			assert(
				conv(
					JsonParser("""{ "water": "1l" }"""),
					typeOf[VesselContent],
					Map(
						"water#" -> water
					))
					=== VesselContent.fromVolume(water, LiquidVolume.l(1))
			)
		}
		it("should parse VesselState") {
			val t1 = Vessel("t1", None)
			assert(
				conv(
					JsonParser("""{ "id": "T1", "content": { "water": "100ul" } }"""),
					typeOf[VesselState],
					Map(
						"vessel" -> t1,
						"content.water#" -> water
					))
					=== VesselContent.fromVolume(water, LiquidVolume.ul(100)).map(VesselState(t1, _, None))
			)
			assert(
				conv(
					JsonParser("""{ "id": "T1", "content": { "water": "100ul" }, "isInitialVolumeKnown": true }"""),
					typeOf[VesselState],
					Map(
						"vessel" -> t1,
						"content.water#" -> water
					))
					=== VesselContent.fromVolume(water, LiquidVolume.ul(100)).map(VesselState(t1, _, Some(true)))
			)
			assert(
				conv(
					JsonParser("""{"id": "T1", "content": {} }"""),
					typeOf[VesselState],
					Map(
						"vessel" -> t1
					))
					=== RqSuccess(VesselState(t1, VesselContent.Empty))
			)
		}
	}

	describe("conv for database objects") {
		import Config01._
		
		val tipState = TipState.createEmpty(tip1)
		val plateState_P1 = PlateState(plate_P1, Some(plateLocation_cooled1))
		val plateState_15000 = PlateState(plate_15000, Some(plateLocation_15000))
		val vesselState_T1 = VesselState(vessel_T1, VesselContent.Empty)
		val vesselState_P1_A01 = VesselState(vessel_P1_A01, VesselContent.fromVolume(water, LiquidVolume.ul(100)).getOrElse(null))
		val vesselSituatedState_T1 = VesselSituatedState(vesselState_T1, VesselPosition(plateState_15000, 0))
	
		val db = new DataBase
		it("should read back same objects as set in the database") {
			def readBack(pair: (String, JsValue)) {
				val (table, JsArray(elements)) = pair
				elements.foreach(jsval => {
					val jsobj = jsval.asJsObject
					val key = jsobj.fields("id").asInstanceOf[JsString].value
					val tkp = TKP(table, key, Nil)
					db.set(tkp, jsval)
					if (db.get(tkp) != RqSuccess(jsval))
						logger.debug(db.toString)
					assert(db.get(tkp) === RqSuccess(jsval))
				})
			}
			Config01.benchJson.fields.foreach(readBack)
			Config01.database1Json.fields.foreach(readBack)
			Config01.protocol1Json.fields.foreach(readBack)

			// Also add tip state
			val tipStateKey = TKP("tipState", "TIP1", Nil)
			val tipStateJson = ConversionsDirect.toJson(tipState).getOrElse(null)
			db.setAt(tipStateKey, List(0), tipStateJson)
			assert(db.getAt(tipStateKey, List(0)) === RqSuccess(tipStateJson))
			logger.debug(db.toString)
		}
		
		def check[A <: Object : TypeTag](id: String, exp: A) = {
			val typ = ru.typeTag[A].tpe
			it(s"should parse $typ `$id`") {
				val table = ConversionsDirect.findTableForType(typ).getOrElse(null)
				val time = if (table.endsWith("State")) List(0) else Nil
				val kc = KeyClass(TKP(table, id, Nil), typ, time)
				val ret = Conversions.readAnyAt(db, kc)
				assert(ret === RqSuccess(exp))
			}
		}

		check[TipModel]("Standard 1000ul", tipModel1000)
		check[Tip]("TIP1", tip1)
		check[PlateModel]("D-BSSE 96 Well PCR Plate", plateModel_PCR)
		check[PlateModel]("Reagent Cooled 8*15ml", plateModel_15000)
		check[PlateLocation]("cool1PCR", plateLocation_cooled1)
		check[PlateLocation]("reagents15000", plateLocation_15000)
		check[TubeModel]("Tube 15000ul", tubeModel_15000)
		check[Plate]("P_1", plate_P1)
		check[Plate]("P_reagents15000", plate_15000)
		check[TipState]("TIP1", tipState)
		check[PlateState]("P_1", plateState_P1)
		check[Vessel]("P_1(A01)", vessel_P1_A01)
		check[Vessel]("T_1", vessel_T1)
		check[VesselState]("P_1(A01)", vesselState_P1_A01)
		check[VesselState]("T_1", vesselState_T1)
		check[VesselSituatedState]("T_1", vesselSituatedState_T1)
	}

	describe("toJson and back") {
		def check[A: TypeTag](l: A*) = {
			val typ = ru.typeTag[A].tpe
			it(s"should parse $typ") {
				for (o <- l) {
					val jsval_? = toJson(o)
					val o2_? = jsval_?.flatMap(conv(_, typ))
					assert(o2_? === RqSuccess(o))
				}
			}
		}
	
		check("Hello, World", "")
		check(0, 42)
		check(0.0, 42.0)
		check(List(1, 2, 3), Nil)
		check[Map[String, Int]](Map("a" -> 1, "b" -> 2), Map())
		check(MyClass1("text", 42, List(true, false)))
	}
}