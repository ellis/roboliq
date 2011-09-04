
package bsse

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

import evoware._

/*
class Tester extends Protocol {
	val water = new Liquid("water", true, false, Set())
	val plate = new Plate
	
	protocol {
		pipette(water, plate, 30 ul)
	}
	
	customize {
		val p2 = new Plate
		
		//water.liquidClass = "water"
		
		plate.label = "P1"
		plate.location = "P1"
		plate.setDimension(4, 1)
		
		p2.label = "P2"
		p2.location = "P2"
		p2.setDimension(8, 1)
		for (well <- p2.wells) {
			val setup = kb.getWellSetup(well)
			setup.liquid_? = Some(water)
			setup.nVolume_? = Some(1000)
		}
		
		kb.addPlate(p2, true)
		/*setInitialLiquids(
			water -> p2.well(1)
		)*/
	}
}

class Tester2 extends Protocol {
	val water = new Liquid("water", false, false, Set())
	val liquid_plasmidDna = new Liquid("plasmid", false, false, Set(Contaminant.DNA))
	val liquid_competentCells = new Liquid("cells", false, false, Set(Contaminant.Cell))
	val liquid_ssDna = new Liquid("ssDNA", false, false, Set(Contaminant.DNA))
	val liquid_liAcMix = new Liquid("LiAcMix", false, false, Set(Contaminant.Other))
	
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
	val robot = BsseRobot()
	
	val evowareMapper = BsseEvowareMapper()
	val translator = new EvowareTranslator(evowareMapper)

	val protocol = new examples.Example01
	if (protocol.m_protocol.isDefined) protocol.m_protocol.get()
	protocol.__findPlateLabels()
	if (protocol.m_customize.isDefined) protocol.m_customize.get()
	robot.devices.foreach(_.addKnowledge(protocol.kb))
	
	val compiler = new Compiler(robot.processors)

	Compiler.compile(protocol.kb, Some(compiler), Some(translator), protocol.cmds) match {
		case Left(err) => err.print()
		case Right(succ) => succ.print()
	}
}
