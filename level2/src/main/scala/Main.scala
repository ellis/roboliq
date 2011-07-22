import scala.collection.mutable.ArrayBuffer

import roboliq.builder.parts._
import roboliq.devices._

class Volume(n: Double) {
	def ul = n
}

trait Roboliq {
	val kb = new KnowledgeBase
	val cmds = new ArrayBuffer[Command]
	
	def pipette(source: WellOrPlateOrLiquid, dest: WellOrPlate, volume: Double) {
		val item = new PipetteItem(source, dest, volume)
		val cmd = PipetteCommand(Seq(item))
		cmds += cmd
	}
	
	def mix(dest: WellOrPlate, volume: Double, count: Int) {
		
	}
	
	implicit def wellToWPL(o: Well): WellOrPlateOrLiquid = WPL_Well(o)
	implicit def plateToWPL(o: Plate): WellOrPlateOrLiquid = WPL_Plate(o)
	implicit def liquidToWPL(o: Liquid): WellOrPlateOrLiquid = WPL_Liquid(o)
	
	implicit def wellToWP(o: Well): WellOrPlate = WP_Well(o)
	implicit def plateToWP(o: Plate): WellOrPlate = WP_Plate(o)
	
	implicit def intToVolume(n: Int): Volume = new Volume(n)
}

class Tester extends Roboliq {
	val water = new Liquid
	val plate = new Plate
	
	pipette(water, plate, 30 ul)
	//pipette(water -> plate, 30 ul)
	//pipette water -> plate volume 30
	//pipette(source = water, dest = plate, volume = 30 ul)
	//pipette water to plate volume 30
}

class Tester2 extends Roboliq {
	val water = new Liquid
	val liquid_plasmidDna = new Liquid
	val liquid_competentCells = new Liquid
	val liquid_ssDna = new Liquid
	val liquid_liAcMix = new Liquid
	
	val plate_template = new Plate
	val plate_working = new Plate
	
	def decontamination_WashBigTips() {
		
	}
	
	def pcrDispense(volume: Double) {
		decontamination_WashBigTips()
		pipette(plate_template, plate_working, volume)
		decontamination_WashBigTips()
	}
	
	def competentYeastDispense() {
		pipette(liquid_plasmidDna, plate_working, 2)
		pipette(liquid_competentCells, plate_working, 30)
		pipette(liquid_ssDna, plate_working, 5)
		pipette(liquid_liAcMix, plate_working, 90)
		mix(plate_working, 90, 4)
	}
	
	def heatShock() {
		
	}
	
	pcrDispense(3)
	competentYeastDispense()
} 

object Main extends App {
	val tester = new Tester
	val handler = new PipetteCommandHandler(tester.kb, tester.cmds.head.asInstanceOf[PipetteCommand])
	val res = handler.exec()
	println(res)
	tester.kb.printUnknown()
}
