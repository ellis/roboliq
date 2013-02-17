package roboliq.processor2

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import org.scalatest.FunSpec
import spray.json._
import roboliq.core._
import ConversionsDirect._


object A extends Enumeration {
	val B, C = Value
}

class ConversionsSpec extends FunSpec {
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
				JsonParser("""{ "idVessel": "T1" }""") -> VesselContent("T1", Map(), Map())
			),
			List(JsNull)
		)
		/*
		it("should parse String") {
			check(JsString("test"), "test")
			check(JsString("test"), "test")
		}
		it("should parse Int") {
			check(JsNumber(42), 42)
		}
			check(JsNumber(42), 42.asInstanceOf[Integer])
		it("should parse Enum") {
			assert(conv(JsString("B"), typeOf[A.Value]) === RqSuccess(A.B))
			assert(conv(JsString("C"), typeOf[A.Value]) === RqSuccess(A.C))
			assert(conv(JsString(""), typeOf[A.Value]).isError)
			assert(conv(JsNumber(1), typeOf[A.Value]).isError)
			assert(conv(JsNull, typeOf[A.Value]).isError)
		}*/
	}
	
	describe("convRequirements") {
		/*def checkList[A: TypeTag](jsval: JsValue, lookup_m: Map[String, Object]succeed_l: List[(JsValue, A)], fail_l: List[JsValue]) = {
			val typ = ru.typeTag[A].tpe
			it(s"should parse $typ") {
				for (pair <- succeed_l) {
					assert(conv(pair._1, typ) === RqSuccess(pair._2))
				}
				for (jsval <- fail_l) {
					assert(conv(jsval, typ).isError)
				}
			}
		}*/
	/*val idVessel: String,
	val solventToVolume: Map[SubstanceLiquid, LiquidVolume],
	val soluteToMol: Map[SubstanceSolid, BigDecimal]
		check[VesselContent](
			List(
				JsonParser("""{"idVessel": "water", "kind": "liquid", "physicalProperties": "Water", "cleanPolicy": {"enter": "Thorough", "within": "None", "exit": "Light"}}""") -> SubstanceLiquid("water", LiquidPhysicalProperties.Water, TipCleanPolicy.TNL, None)
			),
			List(JsNull)
		)*/
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
					=== RqSuccess(VesselContent("T1", Map(water -> LiquidVolume.ul(100)), Map()))
			)
		}
		it("should parse VesselState") {
			val water = SubstanceLiquid("water", LiquidPhysicalProperties.Water, TipCleanPolicy.TL, None)
			val t1 = Vessel0("t1", None)
			assert(
				conv(
					JsonParser("""{ "id": "T1", "content": { "idVessel": "T1", "solventToVolume": { "water": "100ul" } } }"""),
					typeOf[VesselState],
					Map(
						"vessel" -> t1,
						"content.solventToVolume.water#" -> water
					))
					=== RqSuccess(VesselState(t1, VesselContent("T1", Map(water -> LiquidVolume.ul(100)), Map())))
			)
		}
	}
	
	describe("conv for database objects") {
		val config = JsonParser(
"""{
"tipModel": [
	{ "id": "Standard 50ul", "volume": "45ul", "volumeMin": "0.01ul" },
	{ "id": "Standard 1000ul", "volume": "950ul", "volumeMin": "4ul" }
],
"tip": [
	{ "id": "TIP1", "index": 0, "permanent": "Standard 1000ul" },
	{ "id": "TIP2", "index": 1, "permanent": "Standard 1000ul" },
	{ "id": "TIP3", "index": 2, "permanent": "Standard 1000ul" },
	{ "id": "TIP4", "index": 3, "permanent": "Standard 1000ul" },
	{ "id": "TIP5", "index": 4, "permanent": "Standard 50ul" },
	{ "id": "TIP6", "index": 5, "permanent": "Standard 50ul" },
	{ "id": "TIP7", "index": 6, "permanent": "Standard 50ul" },
	{ "id": "TIP8", "index": 7, "permanent": "Standard 50ul" }
],
"plateModel": [
	{ "id": "Reagent Cooled 8*50ml", "rows": 8, "cols": 1, "wellVolume": "50ml" },
	{ "id": "Reagent Cooled 8*15ml", "rows": 8, "cols": 1, "wellVolume": "15ml" },
	{ "id": "Block 20Pos 1.5 ml Eppendorf", "rows": 4, "cols": 5, "wellVolume": "1.5ml" },
	{ "id": "D-BSSE 96 Well PCR Plate", "rows": 8, "cols": 12, "wellVolume": "200ul" },
	{ "id": "D-BSSE 96 Well Costar Plate", "rows": 8, "cols": 12, "wellVolume": "350ul" },
	{ "id": "D-BSSE 96 Well DWP", "rows": 8, "cols": 12, "wellVolume": "1000ul" },
	{ "id": "Trough 100ml", "rows": 8, "cols": 1, "wellVolume": "100ul" },
	{ "id": "Ellis Nunc F96 MicroWell", "rows": 8, "cols": 12, "wellVolume": "400ul" }
],
"plateLocation": [
	{ "id": "trough1", "plateModels": ["Trough 100ml"] },
	{ "id": "trough2", "plateModels": ["Trough 100ml"] },
	{ "id": "trough3", "plateModels": ["Trough 100ml"] },
	{ "id": "reagents15000", "plateModels": ["Reagent Cooled 8*15ml"], "cooled": true },
	{ "id": "uncooled2_low", "plateModels": ["D-BSSE 96 Well DWP", "Ellis Nunc F96 MicroWell"] },
	{ "id": "uncooled2_high", "plateModels": ["D-BSSE 96 Well Costar Plate"] },
	{ "id": "shaker", "plateModels": ["D-BSSE 96 Well Costar Plate", "D-BSSE 96 Well DWP"] },
	{ "id": "cooled1", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled2", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled3", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled4", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled5", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "regrip", "plateModels": ["D-BSSE 96 Well PCR Plate", "D-BSSE 96 Well Costar Plate"] },
	{ "id": "reader", "plateModels": ["D-BSSE 96 Well Costar Plate"] }
],
"tubeModel": [
	{ "id": "Tube 50000ul", "volume": "50000ul" },
	{ "id": "Tube 15000ul", "volume": "15000ul" },
	{ "id": "Tube 1500ul", "volume": "1500ul" }
],
"tubeLocation": [
	{ "id": "reagents50", "tubeModels": ["Tube 50000ul"], "rackModel": "Reagent Cooled 8*50ml" },
	{ "id": "reagents15000", "tubeModels": ["Tube 15000ul"], "rackModel": "Reagent Cooled 8*15ml" },
	{ "id": "reagents1.5", "tubeModels": ["Tube 1500ul"], "rackModel": "Block 20Pos 1.5 ml Eppendorf" }
],
"plate": [
	{ "id": "reagents50", "model": "Reagent Cooled 8*50ml", "location": "reagents50" },
	{ "id": "reagents15000", "model": "Reagent Cooled 8*15ml", "location": "reagents15000" },
	{ "id": "reagents1.5", "model": "Block 20Pos 1.5 ml Eppendorf", "location": "reagents1.5" },
	{ "id": "P1", "model": "D-BSSE 96 Well PCR Plate" }
],
"vessel": [
	{ "id": "T1", "tubeModel": "Tube 15000ul" },
	{ "id": "P1(A01)" }
],

"plateState": [
	{ "id": "reagents15000", "location": "reagents15000" },
	{ "id": "P1", "location": "cooled1" }
],
"vesselState": [
	{ "id": "T1", "content": { "idVessel": "T1" } },
	{ "id": "P1(A01)", "content": { "idVessel": "T1", "solventToVolume": { "water": "100ul" } } }
],
"vesselSituatedState": [
	{ "id": "T1", "position": { "plate": "reagents15000", "index": 0 } },
	{ "id": "P1(A01)", "position": { "plate": "P1", "index": 0 } }
]
}""").asJsObject

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
		val vesselState_T1 = VesselState(vessel_T1, new VesselContent("T1", Map(), Map()))
		val vesselSituatedState_T1 = VesselSituatedState(vesselState_T1, VesselPosition(plateState_15000, 0))
	
		val db = new DataBase
		it("should read back same objects as set in the database") {
			config.fields.foreach(pair => {
				val (table, JsArray(elements)) = pair
				elements.foreach(jsval => {
					val jsobj = jsval.asJsObject
					val key = jsobj.fields("id").asInstanceOf[JsString].value
					val tkp = TKP(table, key, Nil)
					db.set(tkp, Nil, jsval)
					if (db.get(tkp, Nil) != RqSuccess(jsval))
						println(db.toString)
					assert(db.get(tkp, Nil) === RqSuccess(jsval))
				})
			})
			// Also add tip state
			val tipStateKey = TKP("tipState", "TIP1", Nil)
			val tipStateJson = Conversions.tipStateToJson(tipState)
			db.set(tipStateKey, List(0), tipStateJson)
			assert(db.get(tipStateKey, List(0)) === RqSuccess(tipStateJson))
			println(db.toString)
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
		/*
		println("-----------")
		println(read(KeyClass(TKP("tip", "TIP1", Nil), typeOf[Tip])))
		
		read(KeyClass(TKP("tip", "TIP1", Nil), typeOf[Tip])).foreach { o =>
			val tip1 = o.asInstanceOf[Tip]
			println("****************")
			println((tip.id, tip1.id, tip.id == tip1.id))
			println((tip.index, tip1.index, tip.index == tip1.index))
			println((tip.modelPermanent_?, tip1.modelPermanent_?, tip.modelPermanent_? == tip1.modelPermanent_?))
		}*/
	}
}