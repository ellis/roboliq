package roboliq.commands
import org.scalatest.FeatureSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.GivenWhenThen
import spray.json._
import _root_.roboliq._
import _root_.roboliq.core._
import _root_.roboliq.processor._
import _root_.roboliq.processor.ConversionsDirect._
import roboliq.test.Config01


class MovePlateSpec extends FeatureSpec with GivenWhenThen with BeforeAndAfter {
	val movePlate = JsonParser("""{ "cmd": "arm.movePlate", "plate": "P1", "dest": "cooled2", "deviceId": "ROMA2" }""").asJsObject
	val aspirate = JsonParser("""{ "cmd": "pipetter.aspirate", "items": [{"tip": "TIP1", "well": "P1(A01)", "volume": "50ul", "policy": { "id": "Wet", "pos": "WetContact" }}] }""").asJsObject
	var p: ProcessorData = _
	
	before {
		p = new ProcessorData(List(
			new arm.MovePlateHandler,
			new roboliq.commands.pipette.low.AspirateHandler,
			new roboliq.commands.pipette.low.DispenseHandler,
			new roboliq.commands.pipette.low.MixHandler
		))
	
		p.loadJsonData(Config01.benchJson)
		p.loadJsonData(Config01.protocol1Json)
	}
/*
	feature("The user can move plates") {
		
		scenario("script to move a plate") {
			Given("a)
		}
		it("MovePlate") {
			p.setCommands(List(movePlate))
			p.run()
			println(p.getTokenList)
			assert(true === true)
			assert(p.getMessages === Nil)
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
		}
	}

	describe("AspirateHandler") {
	
		it("should run") {
			val p = new ProcessorData(List(
				new AspirateHandler
			))
		
			p.loadJsonData(Config.config01)
	
			val movePlate = JsonParser("""{ "cmd": "arm.movePlate", "plate": "P1", "dest": "cooled2", "deviceId": "ROMA2" }""").asJsObject
			val aspirate = JsonParser("""{ "cmd": "pipetter.aspirate", "items": [{"tip": "TIP1", "well": "P1(A01)", "volume": "50ul", "policy": { "id": "Wet", "pos": "WetContact" }}] }""").asJsObject
			p.setCommands(List(movePlate))
			p.run()
			println(p.getTokenList)
			assert(true === true)
			assert(p.getMessages === Nil)
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
		}
	}
*/	
}