package roboliq.processor2

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import grizzled.slf4j.Logger
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.GivenWhenThen
import spray.json._
import _root_.roboliq.core._
import ConversionsDirect._
import _root_.roboliq.commands2._
import _root_.roboliq._
import roboliq.commands.pipette.TipWellVolumePolicy


class ProcessorSpec extends FunSpec with GivenWhenThen {
	describe("A Processor") {
		val tipModel1 = TipModel("TIPMODEL1", LiquidVolume.ul(950), LiquidVolume.ul(4))
		val tip1 = Tip("TIP1", 0, Some(tipModel1))
		val plateModel1 = PlateModel("PLATEMODEL1", 8, 12, LiquidVolume.ul(200))
		val plateLocation1 = PlateLocation("PLATELOCATION1", List(plateModel1), true)
		val plateLocation2 = PlateLocation("PLATELOCATION2", List(plateModel1), true)
		val tubeModel1 = TubeModel("TUBEMODEL1", LiquidVolume.ml(15))
		val plate1 = Plate("PLATE1", plateModel1, None)
		val plate2 = Plate("PLATE2", plateModel1, None)
		val vessel_TUBE1 = Vessel0("TUBE1", Some(tubeModel1))
		val tipState = TipState0.createEmpty(tip1)
		val plateState_P1 = PlateState(plate1, Some(plateLocation1))
		val plateState_15000 = PlateState(plate2, Some(plateLocation2))
		val vesselState_T1 = VesselState(vessel_TUBE1, new VesselContent(Map(), Map()))
		val vesselSituatedState_T1 = VesselSituatedState(vesselState_T1, VesselPosition(plateState_15000, 0))
		
		def makeTable[A <: Object : TypeTag](a_l: A*): RqResult[(String, JsArray)] = {
			val l: List[A] = a_l.toList
			for {
				table <- ConversionsDirect.findTableForType(ru.typeTag[A].tpe)
				jsval_l <- RqResult.toResultOfList(l.map(ConversionsDirect.toJson[A]))
			} yield table -> JsArray(jsval_l)
		}
		val l1 : List[RqResult[(String, JsArray)]] = List(
			makeTable[TipModel](tipModel1),
			makeTable[Tip](tip1),
			makeTable[PlateModel](plateModel1),
			makeTable[PlateLocation](plateLocation1, plateLocation2),
			makeTable[TubeModel](tubeModel1),
			makeTable[Plate](plate1, plate2),
			makeTable[Vessel0](vessel_TUBE1),
			makeTable(tipState),
			makeTable(plateState_P1, plateState_15000),
			makeTable(vesselState_T1),
			makeTable(vesselSituatedState_T1)
		)
		//info("l1: "+l1)
		val l_? : RqResult[List[(String, JsArray)]] = RqResult.toResultOfList(l1)
		val jsobj_? = l_?.map(l => JsObject(l.toMap))
		
		val p = new ProcessorData(List(
			new arm.MovePlateHandler,
			new pipette.AspirateHandler,
			new pipette.DispenseHandler,
			new pipette.MixHandler
		))
		
		//info("jsobj_?: "+jsobj_?)
		assert(jsobj_?.isSuccess)
		jsobj_?.foreach(jsobj => {
			//println("jsobj: "+jsobj)
			Given("a specific configuration")
			p.loadJsonData(jsobj)
			//println("p.db:")
			//println(p.db)

			it("should hold a copy of each of those configuration objects") {
				assert(p.getObjFromDbAt[TipModel]("TIPMODEL1", Nil) === RqSuccess(tipModel1))
				assert(p.getObjFromDbAt[Tip]("TIP1", Nil) === RqSuccess(tip1))
				assert(p.getObjFromDbAt[PlateModel]("PLATEMODEL1", Nil) === RqSuccess(plateModel1))
				assert(p.getObjFromDbAt[PlateLocation]("PLATELOCATION1", Nil) === RqSuccess(plateLocation1))
				assert(p.getObjFromDbAt[PlateLocation]("PLATELOCATION2", Nil) === RqSuccess(plateLocation2))
				assert(p.getObjFromDbAt[TubeModel]("TUBEMODEL1", Nil) === RqSuccess(tubeModel1))
				assert(p.getObjFromDbAt[Plate]("PLATE1", Nil) === RqSuccess(plate1))
				assert(p.getObjFromDbAt[Plate]("PLATE2", Nil) === RqSuccess(plate2))
				assert(p.getObjFromDbAt[Vessel0]("TUBE1", Nil) === RqSuccess(vessel_TUBE1))
				assert(p.getObjFromDbAt[TipState0]("TIP1", List(0)) === RqSuccess(tipState))
				assert(p.getObjFromDbAt[PlateState]("PLATE1", List(0)) === RqSuccess(plateState_P1))
				assert(p.getObjFromDbAt[PlateState]("PLATE2", List(0)) === RqSuccess(plateState_15000))
				assert(p.getObjFromDbAt[VesselState]("TUBE1", List(0)) === RqSuccess(vesselState_T1))
				assert(p.getObjFromDbAt[VesselSituatedState]("TUBE1", List(0)) === RqSuccess(vesselSituatedState_T1))
			}
		})
	}
}

class ProcessorBsseSpec extends FunSpec with GivenWhenThen {
	private val logger = Logger[this.type]
	
	val movePlate = JsonParser("""{ "cmd": "arm.movePlate", "plate": "P1", "dest": "cooled2", "deviceId": "ROMA2" }""").asJsObject
	val aspirate = JsonParser("""{ "cmd": "pipetter.aspirate", "items": [{"tip": "TIP1", "well": "P1(A01)", "volume": "50ul", "policy": { "id": "Wet", "pos": "WetContact" }}] }""").asJsObject
	
	private def makeProcessor(nameToJson_l: (String, JsObject)*): ProcessorData = {
		Given("a BSSE configuration (8 fixed tips, 4 large, 4 small)")
		val p = new ProcessorData(List(
			new arm.MovePlateHandler,
			new pipette.AspirateHandler,
			new pipette.DispenseHandler,
			new pipette.MixHandler
		))
		p.loadJsonData(Config.config01)
		
		val (name_l, json_l) = nameToJson_l.unzip
		Given("custom configs: "+name_l.mkString(", "))
		json_l.foreach(p.loadJsonData)

		When("commands are run")
		p.run()
		p
	}

	describe("A Processor") {
		/*
		it("should handle arm.movePlate") {
			val p = makeProcessor(List(movePlate))
			
			Then("there should be no errors or warnings")
			assert(p.getMessages === Nil)
			
			And("correct tokens should be generated")
			val (_, token_l) = p.getTokenList.unzip
			val tipModel = TipModel("Standard 1000ul", LiquidVolume.ul(950), LiquidVolume.ul(4))
			val tip = Tip(0, Some(tipModel))
			val plateModel_PCR = PlateModel("D-BSSE 96 Well PCR Plate", 8, 12, LiquidVolume.ul(200))
			val plateModel_15000 = PlateModel("Reagent Cooled 8*15ml", 8, 1, LiquidVolume.ml(15))
			val plateLocation_cooled1 = PlateLocation("cooled1", List(plateModel_PCR), true)
			val plateLocation_cooled2 = PlateLocation("cooled2", List(plateModel_PCR), true)
			val plateLocation_15000 = PlateLocation("reagents15000", List(plateModel_15000), true)
			val tubeModel_15000 = TubeModel("Tube 15000ul", LiquidVolume.ml(15))
			val plate_15000 = Plate("reagents15000", plateModel_15000, None)
			val plate_P1 = Plate("P1", plateModel_PCR, None)
			assert(token_l === List(
				commands2.arm.MovePlateToken(Some("ROMA2"), plate_P1, plateLocation_cooled1, plateLocation_cooled2)
			))
		}*/

		it("should handle pipette.aspirate") {
			import roboliq.commands.pipette._
			import roboliq.commands2.pipette._
			
			val p = makeProcessor(
				"database" -> JsonParser(
					"""{
					"substance": [
						{ "id": "water", "kind": "liquid", "physicalProperties": "Water", "cleanPolicy": {"enter": "Thorough", "within": "None", "exit": "Light"}}
					]
					}""").asJsObject,
				"labware" -> JsonParser(
					"""{
					"plate": [
						{ "id": "P1", "model": "D-BSSE 96 Well PCR Plate" }
					]
					}""").asJsObject,
				"states" -> JsonParser(
					"""{	
					"plateState": [
						{ "id": "P1", "location": "cooled1" }
					],
					"vesselState": [
						{ "id": "P1(A01)", "content": { "idVessel": "T1", "solventToVolume": { "water": "100ul" } } }
					],
					"vesselSituatedState": [
					  { "id": "P1(A01)", "position": { "plate": "P1", "index": 0 } }
					]
					}""").asJsObject,
				"commands" -> JsonParser("""{
					"cmd": [
					  { "cmd": "pipetter.aspirate", "items": [{"tip": "TIP1", "well": "P1(A01)", "volume": "50ul", "policy": { "id": "Wet", "pos": "WetContact" }}] }
					]
					}""").asJsObject
			)
			
			Then("there should be no errors or warnings")
			assert(p.getMessages === Nil)
			
			And("correct tokens should be generated")

			val (_, token_l) = p.getTokenList.unzip
			val tipModel = TipModel("Standard 1000ul", LiquidVolume.ul(950), LiquidVolume.ul(4))
			val tip = Tip(0, Some(tipModel))
			val plateModel_PCR = PlateModel("D-BSSE 96 Well PCR Plate", 8, 12, LiquidVolume.ul(200))
			val plateModel_15000 = PlateModel("Reagent Cooled 8*15ml", 8, 1, LiquidVolume.ml(15))
			val plateLocation_cooled1 = PlateLocation("cooled1", List(plateModel_PCR), true)
			val plateLocation_cooled2 = PlateLocation("cooled2", List(plateModel_PCR), true)
			val plateLocation_15000 = PlateLocation("reagents15000", List(plateModel_15000), true)
			val tubeModel_15000 = TubeModel("Tube 15000ul", LiquidVolume.ml(15))
			val plate_15000 = Plate("reagents15000", plateModel_15000, None)
			val plate_P1 = Plate("P1", plateModel_PCR, None)
			println("token_l: "+token_l)
			val vss_P1_A01_? = p.getObjFromDbAt[VesselSituatedState]("P1(A01)", List(Int.MaxValue))
			println("vss_P1_A01_?: " + vss_P1_A01_?)
			assert(vss_P1_A01_?.isSuccess)
			val vss_P1_A01 = vss_P1_A01_?.getOrElse(null)
			assert(token_l === List(
				roboliq.commands2.pipette.AspirateToken(List(new TipWellVolumePolicy(tip, vss_P1_A01, LiquidVolume.ul(50), PipettePolicy("Wet", PipettePosition.WetContact))))
			))
		}
	}
	
}