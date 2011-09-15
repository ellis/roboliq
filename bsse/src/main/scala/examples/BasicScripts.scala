package examples

import roboliq.common
import roboliq.labs.bsse.Protocol


class AbstractExample01 extends Protocol {
	val ddw = new Liquid("Water")
	val plate1 = new Plate
	
	pipette(ddw, plate1, 30 ul)
	mix(plate1, 30 ul, 5)
}

class Example01(station: roboliq.labs.bsse.station1.StationConfig) extends AbstractExample01 {
	val lab = new EvowareLab {
		import station._
		//ddw.setup.group_? = Some(new roboliq.common.LiquidGroup(CleanPolicy.DDD))
		reagent(ddw, Labwares.reagents15, 1, 8)
		labware(plate1, Sites.cooled1, LabwareModels.platePcr)
	}
}

object LiquidChangeScale extends Enumeration {
	val SameLiquid, SameGroup, SameNothing = Value
}

class Example02(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
	//val water = new Liquid("water", false, false, Set())
	val liquid_plasmidDna = new Liquid("plasmid", Set(Contaminant.DNA), CleanPolicy.DDD)
	val liquid_competentCells = new Liquid("cells", Set(Contaminant.Cell), CleanPolicy.DDD)
	val liquid_ssDna = new Liquid("ssDNA", Set(Contaminant.DNA))
	val liquid_liAcMix = new Liquid("LiAcMix", Set(Contaminant.Other))
	
	val plate_template = new Plate
	val plate_working = new Plate
	
	def competentYeastDispense() {
		pipette(liquid_plasmidDna, plate_working, 2)
		pipette(liquid_competentCells, plate_working, 30)
		pipette(liquid_ssDna, plate_working, 5)
		pipette(liquid_liAcMix, plate_working, 90)
		mix(plate_working, 90, 4)
	}
	
	def incubate(restSeconds: Int, shakeSeconds: Int, count: Int) {
		val location = saveLocation(plate_working)
		for (i <- 0 until count) {
			wait(restSeconds)
			shake(plate_working, shakeSeconds)
			movePlate(plate_working, location)
		}
	}
	
	pipette(plate_template, plate_working, 3)
	competentYeastDispense()
	incubate(3*60, 60, 4)

	val lab = new EvowareLab {
		import station._
		
		reagent(liquid_plasmidDna, Labwares.eppendorfs, 1)
		reagent(liquid_ssDna, Labwares.eppendorfs, 2)
		reagent(liquid_competentCells, Labwares.reagents50, 1)
		reagent(liquid_liAcMix, Labwares.reagents50, 2)
		//labware(plate_template, Sites.cooled1, LabwareModels.platePcr)
		//labware(plate_working, Sites.cooled2, LabwareModels.platePcr)
		labware(plate_template, Sites.cooled1, LabwareModels.test4x3)
		labware(plate_working, Sites.cooled2, LabwareModels.test4x3)
		new roboliq.common.PlateProxy(kb, plate_template) match { case pp =>
			for (wellObj <- pp.wells) {
				val wellSetup = kb.getWellSetup(wellObj)
				val sLiquid = "template#"+wellSetup.index_?.get
				val liquid = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
				liquid.setup.sName_? = Some(sLiquid)
				wellSetup.reagent_? = Some(liquid)
			}
		}
	}
}
