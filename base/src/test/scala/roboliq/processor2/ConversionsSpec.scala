package roboliq.processor2

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import grizzled.slf4j.Logger
import org.scalatest.FunSpec
import spray.json._
import roboliq.core._
import ConversionsDirect._


object A extends Enumeration {
	val B, C = Value
}

class ConversionsSpec extends FunSpec {
	private val logger = Logger[this.type]
	
	private def getTypeTag[T: TypeTag](obj: T) = ru.typeTag[T]
	private def getType[T: TypeTag](obj: T) = ru.typeTag[T].tpe

	//def check[A: TypeTag](jsval: JsValue, exp: A) =
	//	assert(ConversionsDirect.conv(jsval, ru.typeTag[A].tpe) == RqSuccess(exp))

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
				JsonParser("""{"id": "water", "kind": "liquid", "physicalProperties": "Water", "cleanPolicy": {"enter": "Thorough", "within": "None", "exit": "Light"}}""") -> SubstanceLiquid("water", LiquidPhysicalProperties.Water, TipCleanPolicy.TL, None)
			),
			List(JsNull)
		)
		check[VesselContent](
			List(
				JsonParser("""{}""") -> VesselContent(Map(), Map())
			),
			List(JsNull)
		)
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
			val water = SubstanceLiquid("water", LiquidPhysicalProperties.Water, TipCleanPolicy.TL, None)
			val powder = SubstanceOther("powder", Some(3.5))
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
			val water = SubstanceLiquid("water", LiquidPhysicalProperties.Water, TipCleanPolicy.TL, None)
			val powder = SubstanceOther("powder", Some(3.5))
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
		it("should parse VesselContent") {
			val water = SubstanceLiquid("water", LiquidPhysicalProperties.Water, TipCleanPolicy.TL, None)
			assert(
				conv(
					JsonParser("""{ "idVessel": "T1", "solventToVolume": { "water": "100ul" } }"""),
					typeOf[VesselContent],
					Map(
						"solventToVolume.water#" -> water
					))
					=== RqSuccess(VesselContent(Map(water -> LiquidVolume.ul(100)), Map()))
			)
		}
		it("should parse VesselState") {
			val water = SubstanceLiquid("water", LiquidPhysicalProperties.Water, TipCleanPolicy.TL, None)
			val t1 = Vessel0("t1", None)
			assert(
				conv(
					JsonParser("""{ "id": "T1", "content": { "solventToVolume": { "water": "100ul" } } }"""),
					typeOf[VesselState],
					Map(
						"vessel" -> t1,
						"content.solventToVolume.water#" -> water
					))
					=== RqSuccess(VesselState(t1, VesselContent(Map(water -> LiquidVolume.ul(100)), Map())))
			)
			assert(
				conv(
					JsonParser("""{"id":"T1","content":{"solventToVolume":{},"soluteToMol":{}}}"""),
					typeOf[VesselState],
					Map(
						"vessel" -> t1
					))
					=== RqSuccess(VesselState(t1, VesselContent(Map(), Map())))
			)
		}
	}
	
	describe("conv for database objects") {
		val tipModel = TipModel("Standard 1000ul", LiquidVolume.ul(950), LiquidVolume.ul(4))
		val tip = Tip(0, Some(tipModel))
		val plateModel_PCR = PlateModel("D-BSSE 96 Well PCR Plate", 8, 12, LiquidVolume.ul(200))
		val plateModel_15000 = PlateModel("Reagent Cooled 8*15ml", 8, 1, LiquidVolume.ml(15))
		val plateLocation_cooled1 = PlateLocation("cooled1", List(plateModel_PCR), true)
		val plateLocation_15000 = PlateLocation("reagents15000", List(plateModel_15000), true)
		val tubeModel_15000 = TubeModel("Tube 15000ul", LiquidVolume.ml(15))
		val plate_15000 = Plate("reagents15000", plateModel_15000, None)
		val plate_P1 = Plate("P1", plateModel_PCR, None)
		val vessel_T1 = Vessel0("T1", Some(tubeModel_15000))
		val tipState = TipState0.createEmpty(tip)
		val plateState_P1 = PlateState(plate_P1, Some(plateLocation_cooled1))
		val plateState_15000 = PlateState(plate_15000, Some(plateLocation_15000))
		val vesselState_T1 = VesselState(vessel_T1, new VesselContent(Map(), Map()))
		val vesselSituatedState_T1 = VesselSituatedState(vesselState_T1, VesselPosition(plateState_15000, 0))
	
		val db = new DataBase
		it("should read back same objects as set in the database") {
			Config.config01.fields.foreach(pair => {
				val (table, JsArray(elements)) = pair
				elements.foreach(jsval => {
					val jsobj = jsval.asJsObject
					val key = jsobj.fields("id").asInstanceOf[JsString].value
					val tkp = TKP(table, key, Nil)
					db.set(tkp, Nil, jsval)
					if (db.get(tkp) != RqSuccess(jsval))
						logger.debug(db.toString)
					assert(db.get(tkp) === RqSuccess(jsval))
				})
			})
			// Also add tip state
			val tipStateKey = TKP("tipState", "TIP1", Nil)
			val tipStateJson = Conversions.tipStateToJson(tipState)
			db.set(tipStateKey, List(0), tipStateJson)
			assert(db.getAt(tipStateKey, List(0)) === RqSuccess(tipStateJson))
			logger.debug(db.toString)
		}
		
		def check[A <: Object : TypeTag](id: String, exp: A) = {
			val typ = ru.typeTag[A].tpe
			it(s"should parse $typ `$id`") {
				val kc = KeyClass(TKP(ConversionsDirect.tableForType(typ), id, Nil), typ)
				val ret = Conversions.readAny(db, kc)
				assert(ret === RqSuccess(exp))
			}
		}

		check[TipModel]("Standard 1000ul", tipModel)
		check[Tip]("TIP1", tip)
		check[PlateModel]("D-BSSE 96 Well PCR Plate", plateModel_PCR)
		check[PlateModel]("Reagent Cooled 8*15ml", plateModel_15000)
		check[PlateLocation]("cooled1", plateLocation_cooled1)
		check[PlateLocation]("reagents15000", plateLocation_15000)
		check[TubeModel]("Tube 15000ul", tubeModel_15000)
		check[Plate]("P1", plate_P1)
		check[Plate]("reagents15000", plate_15000)
		check[TipState0]("TIP1", tipState)
		check[PlateState]("P1", plateState_P1)
		check[Vessel0]("T1", vessel_T1)
		check[VesselState]("T1", vesselState_T1)
		check[VesselSituatedState]("T1", vesselSituatedState_T1)
	}
}