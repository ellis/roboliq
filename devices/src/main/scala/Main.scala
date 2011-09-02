import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._


class Tester extends L4_Roboliq with PipetteCommands {
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

object TestRobot {
	def apply(): Robot = {
		val pipetter = new PipetteDeviceGeneric {
			override def addKnowledge(kb: KnowledgeBase) {
				super.addKnowledge(kb)
				val plateDeconAspirate, plateDeconDispense = new Plate
				new PlateProxy(kb, plateDeconAspirate) match {
					case pp =>
						pp.label = "DeconA"
						pp.location = "DeconA"
						pp.setDimension(8, 1)
				}
				new PlateProxy(kb, plateDeconDispense) match {
					case pp =>
						pp.label = "DeconD"
						pp.location = "DeconD"
						pp.setDimension(8, 1)
				}
			}
		}
		
		val devices = Seq(
			pipetter
			)

		val processors = Seq(
			new L3P_TipsReplace,
			new L3P_TipsDrop("WASTE"),
			//new L3P_TipsWash_BSSE(pipetter, plateDeconAspirate, plateDeconDispense),
			new L3P_Pipette(pipetter),
			new L3P_Mix(pipetter)
			)
		
		new Robot(devices, processors)
	}
}

object Main extends App {
	val robot = TestRobot()
	
	val tester = new Tester
	Compiler.compile(robot, tester)
}
