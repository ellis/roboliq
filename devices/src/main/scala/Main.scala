import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.level3._
import roboliq.devices._
import roboliq.devices.pipette._

trait Roboliq extends L3_Roboliq {
	def pipette(source: WellOrPlateOrLiquid, dest: WellOrPlate, volume: Double) {
		val item = new PipetteItem(source, dest, volume)
		val cmd = PipetteCommand(Seq(item))
		cmds += cmd
	}
}

class Tester extends Roboliq {
	val water = new Liquid("water", true, false, false, false, false)
	val plate = new Plate
	
	protocol {
		pipette(water, plate, 30 ul)
	}
	
	customize {
		val p2 = new Plate
		
		//water.liquidClass = "water"
		
		plate.location = "P1"
		plate.setDimension(8, 12)
		
		p2.location = "P2"
		p2.setDimension(8, 1)
		for (well <- p2.wells) {
			val st = kb.getWellState0L3(well)
			st.liquid_? = Some(water)
			st.nVolume_? = Some(1000)
		}
		
		kb.addPlate(p2, true)
		/*setInitialLiquids(
			water -> p2.well(1)
		)*/
	}
}

/*
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
*/

object Main extends App {
	val tester = new Tester
	tester.m_protocol.get()
	tester.m_customize.get()
	
	def createCompiler(): Compiler = {
		val pipetter = new PipetteDeviceGeneric()
		
		val compiler = new Compiler
		compiler.register(new Compiler_PipetteCommand)
		compiler.register(new Compiler_PipetteCommandL2(pipetter))
		compiler.register(new Compiler_AspirateL1)
		compiler.register(new Compiler_DispenseL1)
		compiler
	}

	val compiler = createCompiler()
	tester.kb.concretize() match {
		case Right(map31) =>
			val state0 = new RobotState(map31.state0L1)//.toInstanceOf
			compiler.compileL3(tester.kb, map31, state0, tester.cmds)
		case Left(errors) =>
			println(errors)
	}
	/*val handler = new PipetteCommandHandler(tester.kb, tester.cmds.head.asInstanceOf[PipetteCommand])
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
	//tester.kb.printUnknown()*/
}
