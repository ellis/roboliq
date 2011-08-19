import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

trait Roboliq extends L3_Roboliq {
	def pipette(source: WellOrPlateOrLiquid, dest: WellOrPlate, volume: Double) {
		val item = new L3A_PipetteItem(source, dest, volume)
		val cmd = L3C_Pipette(Seq(item))
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
		plate.setDimension(4, 1)
		
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
	val kb = tester.kb
	
	def createCompiler(): Compiler = {
		val pipetter = new PipetteDeviceGeneric()
		pipetter.config.tips.foreach(kb.addObject)
		
		val compiler = new Compiler
		compiler.register(new L3P_Pipette)
		compiler.register(new L2P_Pipette(pipetter))
		compiler.register(new L1P_Aspirate)
		compiler.register(new L1P_Dispense)
		compiler.register(new L1P_SetTipStateClean)
		compiler
	}
	
	println("Input:")
	tester.cmds.foreach(println)
	println()

	val compiler = createCompiler()
	tester.kb.concretize() match {
		case Right(map31) =>
			val state0 = new RobotState(map31.state0L1)//.toInstanceOf
			val nodes = compiler.compileL3(tester.kb, map31, state0, tester.cmds)
			println("Output:")
			val finals = nodes.flatMap(_.collectFinal())
			finals.map(_.cmd).foreach(println)
		case Left(errors) =>
			println(errors)
	}
}
