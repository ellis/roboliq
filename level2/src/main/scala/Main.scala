import scala.collection.mutable.ArrayBuffer

import roboliq.builder.parts._
import roboliq.devices._


trait Roboliq {
	val kb = new KnowledgeBase
	val cmds = new ArrayBuffer[Command]
	
	def pipette(source: WellOrPlateOrLiquid, dest: WellOrPlate, volume: Double) {
		val item = new PipetteItem(source, dest, volume)
		val cmd = PipetteCommand(Seq(item))
		cmds += cmd
	}
	
	implicit def wellToWPL(o: Well): WellOrPlateOrLiquid = WPL_Well(o)
	implicit def plateToWPL(o: Plate): WellOrPlateOrLiquid = WPL_Plate(o)
	implicit def liquidToWPL(o: Liquid): WellOrPlateOrLiquid = WPL_Liquid(o)
	
	implicit def wellToWP(o: Well): WellOrPlate = WP_Well(o)
	implicit def plateToWP(o: Plate): WellOrPlate = WP_Plate(o)
}

class Tester extends Roboliq {
	val water = new Liquid
	val plate = new Plate
	
	pipette(water, plate, 30)
	//pipette(water -> plate, 30)
	//pipette water -> plate volume 30
	//pipette(source = water, dest = plate, volume = 30)
	//pipette water to plate volume 30
}

object Main extends App {
	val tester = new Tester
	val handler = new PipetteCommandHandler(tester.kb, tester.cmds.head.asInstanceOf[PipetteCommand])
	val res = handler.exec()
	println(res)
	tester.kb.printUnknown()
}
