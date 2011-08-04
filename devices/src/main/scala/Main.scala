import scala.collection.mutable.ArrayBuffer

import roboliq.level3._
import roboliq.devices._

trait Roboliq extends L3_Roboliq {
	def pipette(source: WellOrPlateOrLiquid, dest: WellOrPlate, volume: Double) {
		val item = new PipetteItem(source, dest, volume)
		val cmd = PipetteCommand(Seq(item))
		cmds += cmd
	}
}

class Tester extends Roboliq {
	val water = new Liquid
	val plate = new Plate
	
	protocol {
		pipette(water, plate, 30 ul)
	}
	
	customize {
		val p2 = Plate(rows = 8, cols = 1, location = "P2")

		water.liquidClass = "water"
		
		plate.location = "P1"
		
		p2.location = "P2"
		
		/*setInitialLiquids(
			water -> p2.well(1)
		)*/
	}
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
