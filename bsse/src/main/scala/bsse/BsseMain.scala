package bsse

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

import evoware._


class Tester extends Roboliq {
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

class Tester2 extends Roboliq {
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


object Main extends App {
	val robot = new BsseRobot
	
	val evowareMapper = BsseEvowareMapper()
	val translator = new EvowareTranslator(evowareMapper)

	val tester = new Tester
	Compiler.compile(robot, translator, tester)
	tester.m_protocol.get()
	tester.m_customize.get()
	val kb = tester.kb
	
	def createCompiler(): Compiler = {
		val pipetter = new PipetteDeviceGeneric()
		pipetter.config.tips.foreach(kb.addObject)
		
		val plateDeconAspirate, plateDeconDispense = new Plate
		new PlateProxy(kb, plateDeconAspirate) match {
			case pp =>
				pp.label = "DA"
				pp.location = "DA"
				pp.setDimension(8, 1)
		}
		new PlateProxy(kb, plateDeconDispense) match {
			case pp =>
				pp.label = "DD"
				pp.location = "DD"
				pp.setDimension(8, 1)
		}
		
		val compiler = new Compiler
		//compiler.register(new L4P_Pipette)
		//compiler.register(new L3P_Clean(pipetter, plateDeconAspirate, plateDeconDispense))
		compiler.register(new L3P_TipsReplace)
		compiler.register(new L3P_TipsDrop("waste"))
		compiler.register(new L3P_Pipette(pipetter))
		//compiler.register(new L2P_Aspirate)
		//compiler.register(new L2P_Dispense)
		//compiler.register(new L2P_SetTipStateClean)
		compiler
	}
	
	println("Input:")
	tester.cmds.foreach(println)
	println()

	val compiler = createCompiler()
	tester.kb.concretize() match {
		case Right(map31) =>
			val state0 = map31.createRobotState()
			compiler.compile(state0, tester.cmds) match {
				case Left(err) =>
					println("Compilation errors:")
					err.errors.foreach(println)
				case Right(nodes) =>
					val finals = nodes.flatMap(_.collectFinal())
					println("Output:")
					finals.map(_.cmd).foreach(println)
			}
		case Left(errors) =>
			println("Missing information:")
			println(errors)
	}
}
