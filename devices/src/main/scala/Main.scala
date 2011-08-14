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
		val p2 = new Plate
		
		water.liquidClass = "water"
		
		plate.location = "P1"
		plate.setDimension(8, 12)
		
		p2.location = "P2"
		p2.setDimension(8, 1)
		p2.wells.foreach(_.fill(water, 1000))
		
		kb.addPlate(p2, true)
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
	tester.m_protocol.get()
	tester.m_customize.get()
	
	val handler = new PipetteCommandHandler(tester.kb, tester.cmds.head.asInstanceOf[PipetteCommand])
	handler.addKnowledge()
	val missing = tester.kb.concretize()
	if (missing.isEmpty) {
		val errs = handler.checkParams()
		if (errs.isEmpty) {
			val res = handler.compile()
			println(res)
		}
		else {
			println(errs)
		}
	}
	else {
		println(missing)
	}
	//tester.kb.printUnknown()
}
