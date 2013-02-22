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


class ProcessorSpec extends FunSpec with GivenWhenThen {
	describe("A Processor") {
		val tipModel = TipModel("Standard 1000ul", LiquidVolume.ul(950), LiquidVolume.ul(4))
		val tip1 = Tip(0, Some(tipModel))
		val plateModel_PCR = PlateModel("D-BSSE 96 Well PCR Plate", 8, 12, LiquidVolume.ul(200))
		val plateModel_15000 = PlateModel("Reagent Cooled 8*15ml", 8, 1, LiquidVolume.ml(15))
		val plateLocation_cooled1 = PlateLocation("cooled1", List(plateModel_PCR), true)
		val plateLocation_15000 = PlateLocation("reagents15000", List(plateModel_15000), true)
		val tubeModel_15000 = TubeModel("Tube 15000ul", LiquidVolume.ml(15))
		val plate_15000 = Plate("reagents15000", plateModel_15000, None)
		val plate_P1 = Plate("P1", plateModel_PCR, None)
		val vessel_T1 = Vessel0("T1", Some(tubeModel_15000))
		val tipState = TipState0.createEmpty(tip1)
		val plateState_P1 = PlateState(plate_P1, Some(plateLocation_cooled1))
		val plateState_15000 = PlateState(plate_15000, Some(plateLocation_15000))
		val vesselState_T1 = VesselState(vessel_T1, new VesselContent(Map(), Map()))
		val vesselSituatedState_T1 = VesselSituatedState(vesselState_T1, VesselPosition(plateState_15000, 0))
		
		val m = List[(String, List[Object])](
			"tipModel" -> List(tipModel),
			"tip" -> List(tip1),
			"plateModel" -> List(plateModel_PCR, plateModel_15000),
			"plateLocation" -> List(plateLocation_cooled1, plateLocation_15000),
			"tubeModel" -> List(tubeModel_15000)
		)
	}
}

class ProcessorBsseSpec extends FunSpec with GivenWhenThen {
	private val logger = Logger[this.type]
	
	val movePlate = JsonParser("""{ "cmd": "arm.movePlate", "plate": "P1", "dest": "cooled2", "deviceId": "ROMA2" }""").asJsObject
	val aspirate = JsonParser("""{ "cmd": "pipetter.aspirate", "items": [{"tip": "TIP1", "well": "P1(A01)", "volume": "50ul", "policy": { "id": "Wet", "pos": "WetContact" }}] }""").asJsObject
	
	private def makeProcessor(cmd_l: List[JsObject]): ProcessorData = {
		Given("a BSSE configuration (8 fixed tips, 4 large, 4 small)")
		info("cmd_l: "+cmd_l)
		val p = new ProcessorData(List(
			new arm.MovePlateHandler,
			new pipette.AspirateHandler,
			new pipette.DispenseHandler,
			new pipette.MixHandler
		))

		When("command is run")
		p.loadJsonData(Config.config01)
		p.setCommands(cmd_l)
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
			val p = makeProcessor(List(aspirate))
			
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
			assert(token_l === List(
				commands2.arm.MovePlateToken(Some("ROMA2"), plate_P1, plateLocation_cooled1, plateLocation_cooled2)
			))
		}
	}
	
}