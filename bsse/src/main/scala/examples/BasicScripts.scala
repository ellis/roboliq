package examples

//import roboliq.common._
//import roboliq.commands.pipette._
//import roboliq.compiler._
//import roboliq.devices.pipette._
import bsse.Protocol

class Example01 extends Protocol {
	val ddw = new Liquid(LiquidFamily.Water)
	val plate1 = new Plate(PlateFamily.Standard)
	
	pipette(ddw, plate1, 30 ul)
	mix(plate1, 30 ul, 5)

	val lab = new EvowareLab with ExampleTable2 {
		reagent(ddw, Labwares.reagents15, 1, 8)
		labware(plate1, Sites.cooled1, LabwareModels.platePcr)
	}
}

object LiquidChangeScale extends Enumeration {
	val SameLiquid, SameGroup, SameNothing = Value
}

class X1 {
	def x(scale: LiquidChangeScale.Value, bContaminated: Boolean) {
		scale match {
			case LiquidChangeScale.SameLiquid =>
				CleanIntensity.None
			case LiquidChangeScale.SameGroup =>
				
			case (LiquidChangeScale.SameNothing, true) => CleanIntensity.Decontaminate
			case (LiquidChangeScale.SameNothing, false) => 
			case (LiquidChangeScale.SameGroup, true) => CleanIntensity.Decontaminate
			case (LiquidChangeScale.SameGroup, false) =>
			case (LiquidChangeScale.SameLiquid, _) => CleanIntensity.Decontaminate
				
		}
	}
}

class Example02 extends Protocol {
	val ddw = new Liquid(LiquidFamily.Water)
	val plate1 = new Plate(PlateFamily.Standard)
	
	pipette(ddw, plate1, 30 ul)
	mix(plate1, 30 ul, 5)

	val lab = new EvowareLab with ExampleTable2 {
		reagent(ddw, Labwares.reagents15, 1, 8)
		labware(plate1, Sites.cooled1, LabwareModels.platePcr)
	}
	
	//val water = new Liquid("water", false, false, Set())
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