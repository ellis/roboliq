package roboliq.processor2

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import grizzled.slf4j.Logger
import org.scalatest.FunSpec
import spray.json._
import _root_.roboliq.core._
import ConversionsDirect._
import _root_.roboliq.commands2.arm.MovePlateHandler
import _root_.roboliq._


class ProcessorSpec extends FunSpec {
	private val logger = Logger[this.type]

	describe("MovePlateHandler") {
	
		it("should run") {
			val p = new ProcessorData(List(
				new MovePlateHandler
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
				commands2.arm.MovePlateToken(Some("ROMA2"), plate_P1, plateLocation_cooled1, plateLocation_cooled2)))
		}
	}
	
}