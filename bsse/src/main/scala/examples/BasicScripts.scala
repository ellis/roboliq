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
		// FIXME: Should we postpone the final cleaning of the above mix operation? 
	}
	
	def incubate(restSeconds: Int, shakeSeconds: Int, count: Int) {
		val location = saveLocation(plate_working)
		for (i <- 0 until count) {
			wait(restSeconds)
			shake(plate_working, shakeSeconds)
			movePlate(plate_working, location)
		}
	}
	
	/*def heatShock(seconds: Int) {
		val location = saveLocation(plate_working)
		movePlate(plate_working, location)
		shake(plate_working, shakeSeconds)
		movePlate(plate_working, location)
	}*/
	
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


class Example03(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
	//import common.WellPointer
	import roboliq.commands.MixItemL4
	import roboliq.commands.MixItemReagentL4
	import roboliq.commands.MixItemTemplateL4
	
	object Liquids {
		val water = new Liquid("Water", CleanPolicy.DDD)
		val buffer10x = new Liquid("Water", CleanPolicy.DDD)
		val dNTP = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerF = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerB = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val polymerase = new Liquid("Glycerol", Set(Contaminant.DNA), CleanPolicy.DDD)
	}
	
	val well_template = new common.WellPointerVar
	val well_masterMix = new common.WellPointerVar
	val plate_working = new Plate
	
	val mixItems = Seq[MixItemL4](
		MixItemReagentL4(Liquids.buffer10x, 10, 1),
		MixItemReagentL4(Liquids.dNTP, 2, .2),
		MixItemReagentL4(Liquids.primerF, 50, .5),
		MixItemReagentL4(Liquids.primerB, 50, .5),
		MixItemReagentL4(Liquids.polymerase, 5, 0.25/25),
		MixItemTemplateL4(well_template, Seq(20), 0.2)
	)
	
	pcrMix(plate_working(B5+2), mixItems, Liquids.water, 50 ul, well_masterMix)
	__findLabels(Liquids)

	val lab = new EvowareLab {
		import station._

		/*kb.addReagent(Liquids.water)
		kb.addReagent(Liquids.buffer10x)
		kb.addReagent(Liquids.dNTP)
		kb.addReagent(Liquids.primerF)
		kb.addReagent(Liquids.primerB)
		kb.addReagent(Liquids.polymerase)*/
		
		reagent(Liquids.water, Labwares.reagents50, 1, 8)
		reagent(Liquids.buffer10x, Labwares.eppendorfs, 1)
		reagent(Liquids.dNTP, Labwares.eppendorfs, 5)
		reagent(Liquids.primerF, Labwares.eppendorfs, 9)
		reagent(Liquids.primerB, Labwares.eppendorfs, 13)
		reagent(Liquids.polymerase, Labwares.eppendorfs, 17)
		
		well_template.pointer_? = Some(Labwares.eppendorfs.commonObj(B1))
		well_masterMix.pointer_? = Some(Labwares.eppendorfs.commonObj(B2))
		
		//labware(plate_template, Sites.cooled1, LabwareModels.platePcr)
		labware(plate_working, Sites.cooled2, LabwareModels.platePcr)
		//labware(plate_template, Sites.cooled1, LabwareModels.test4x3)
		//labware(plate_working, Sites.cooled2, LabwareModels.test4x3)
		for (wells <- well_template.getWells(kb); wellObj <- wells) {
			val wellSetup = kb.getWellSetup(wellObj)
			val sLiquid = "template#"+wellSetup.index_?.get
			val liquid = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
			liquid.setup.sName_? = Some(sLiquid)
			wellSetup.reagent_? = Some(liquid)
		}
	}
}


class Example04(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
	import common.WellPointer
	
	val plate_working = new Plate
	val plate_balance = new Plate
	
	seal(plate_working)
	val setup_thermocycle = thermocycle(plate_working)
	val setup_centrifuge = centrifuge(plate_working)
	peel(plate_working)

	val lab = new EvowareLab {
		import station._

		labware(plate_balance, Sites.cooled1, LabwareModels.platePcr)
		labware(plate_working, Sites.cooled2, LabwareModels.platePcr)
		
		station.centrifuge.setup.plate_balance = plate_balance
		
		setup_thermocycle.program.program = "0,2"
		setup_centrifuge.idProgram_? = Some("2000,15,9,9,20")
	}
}


class Example05(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
	import roboliq.commands.MixItemL4
	import roboliq.commands.MixItemReagentL4
	import roboliq.commands.MixItemTemplateL4
	
	object Liquids {
		val water = new Liquid("Water", CleanPolicy.DDD)
		val buffer10x = new Liquid("Water", CleanPolicy.DDD)
		val dNTP = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerF = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerB = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val polymerase = new Liquid("Glycerol", Set(Contaminant.DNA), CleanPolicy.DDD)
	}
	
	val well_template = new common.WellPointerVar
	val well_masterMix = new common.WellPointerVar
	val plate_working = new Plate
	val plate_balance = new Plate
	
	val mixItems = Seq[MixItemL4](
		MixItemReagentL4(Liquids.buffer10x, 10, 1),
		MixItemReagentL4(Liquids.dNTP, 2, .2),
		MixItemReagentL4(Liquids.primerF, 50, .5),
		MixItemReagentL4(Liquids.primerB, 50, .5),
		MixItemReagentL4(Liquids.polymerase, 5, 0.25/25),
		MixItemTemplateL4(well_template, Seq(20), 0.2)
	)
	
	pcrMix(plate_working(B5+2), mixItems, Liquids.water, 50 ul, well_masterMix)
	seal(plate_working)
	val setup_thermocycle = thermocycle(plate_working)
	val setup_centrifuge = centrifuge(plate_working)
	peel(plate_working)

	__findLabels(Liquids)

	val lab = new EvowareLab {
		import station._

		reagent(Liquids.water, Labwares.reagents50, 1, 8)
		reagent(Liquids.buffer10x, Labwares.eppendorfs, 1)
		reagent(Liquids.dNTP, Labwares.eppendorfs, 5)
		reagent(Liquids.primerF, Labwares.eppendorfs, 9)
		reagent(Liquids.primerB, Labwares.eppendorfs, 13)
		reagent(Liquids.polymerase, Labwares.eppendorfs, 17)
		
		well_template.pointer_? = Some(Labwares.eppendorfs.commonObj(B1))
		well_masterMix.pointer_? = Some(Labwares.eppendorfs.commonObj(B2))
		
		labware(plate_balance, Sites.cooled1, LabwareModels.platePcr)
		labware(plate_working, Sites.cooled2, LabwareModels.platePcr)
		for (wells <- well_template.getWells(kb); wellObj <- wells) {
			val wellSetup = kb.getWellSetup(wellObj)
			val sLiquid = "template#"+wellSetup.index_?.get
			val liquid = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
			liquid.setup.sName_? = Some(sLiquid)
			wellSetup.reagent_? = Some(liquid)
		}
		
		station.centrifuge.setup.plate_balance = plate_balance
		setup_thermocycle.program.program = "0,2"
		setup_centrifuge.idProgram_? = Some("2000,15,9,9,20")
	}
}


class Protocol06 extends Protocol {
	import roboliq.commands.MixItemL4
	import roboliq.commands.MixItemReagentL4
	import roboliq.commands.MixItemTemplateL4
	
	object Liquids {
		val water = new Liquid("Water", CleanPolicy.DDD)
		val buffer10x = new Liquid("Water", CleanPolicy.DDD)
		val dNTP = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerF = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerB = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val polymerase = new Liquid("Glycerol", Set(Contaminant.DNA), CleanPolicy.DDD)
	}
	
	object Plates {
		val working, balance = new Plate
	}
	
	object Wells {
	}
	
	class PcrPreparationProgram {
		val water = new Liquid("Water", CleanPolicy.DDD)
		val buffer = new Liquid("Water", CleanPolicy.DDD)
		val dNTP = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerF = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerB = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val polymerase = new Liquid("Glycerol", Set(Contaminant.DNA), CleanPolicy.DDD)

		var bufferConc: Tuple2[Double, Double] = null
		var dNTPConc: Tuple2[Double, Double] = null
		var primerFConc: Tuple2[Double, Double] = null
		var primerBConc: Tuple2[Double, Double] = null
		var polymeraseConc: Tuple2[Double, Double] = null
		
		val templateWells = new common.WellPointerVar
		val masterMixWells = new common.WellPointerVar
	}
	
	val well_template = new common.WellPointerVar
	val well_masterMix = new common.WellPointerVar
	
	val mixItems = Seq[MixItemL4](
		MixItemReagentL4(Liquids.buffer10x, 10, 1),
		MixItemReagentL4(Liquids.dNTP, 2, .2),
		MixItemReagentL4(Liquids.primerF, 50, .5),
		MixItemReagentL4(Liquids.primerB, 50, .5),
		MixItemReagentL4(Liquids.polymerase, 5, 0.25/25),
		MixItemTemplateL4(well_template, Seq(20), 0.2)
	)
	
	pcrMix(Plates.working(B5+2), mixItems, Liquids.water, 50 ul, well_masterMix)
	seal(Plates.working)
	val setup_thermocycle = thermocycle(Plates.working)
	val setup_centrifuge = centrifuge(Plates.working)
	peel(Plates.working)

	__findLabels(Liquids)
	__findLabels(Plates)
}


class Example06(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
	import roboliq.commands.MixItemL4
	import roboliq.commands.MixItemReagentL4
	import roboliq.commands.MixItemTemplateL4
	
	object Liquids {
		val water = new Liquid("Water", CleanPolicy.DDD)
		val buffer10x = new Liquid("Water", CleanPolicy.DDD)
		val dNTP = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerF = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerB = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val polymerase = new Liquid("Glycerol", Set(Contaminant.DNA), CleanPolicy.DDD)
	}
	
	val well_template = new common.WellPointerVar
	val well_masterMix = new common.WellPointerVar
	val plate_working = new Plate
	val plate_balance = new Plate
	
	val mixItems = Seq[MixItemL4](
		MixItemReagentL4(Liquids.buffer10x, 10, 1),
		MixItemReagentL4(Liquids.dNTP, 2, .2),
		MixItemReagentL4(Liquids.primerF, 50, .5),
		MixItemReagentL4(Liquids.primerB, 50, .5),
		MixItemReagentL4(Liquids.polymerase, 5, 0.25/25),
		MixItemTemplateL4(well_template, Seq(20), 0.2)
	)
	
	pcrMix(plate_working(B5+2), mixItems, Liquids.water, 50 ul, well_masterMix)
	seal(plate_working)
	val setup_thermocycle = thermocycle(plate_working)
	val setup_centrifuge = centrifuge(plate_working)
	peel(plate_working)

	__findLabels(Liquids)

	val lab = new EvowareLab {
		import station._

		reagent(Liquids.water, Labwares.reagents50, 1, 8)
		reagent(Liquids.buffer10x, Labwares.eppendorfs, 1)
		reagent(Liquids.dNTP, Labwares.eppendorfs, 5)
		reagent(Liquids.primerF, Labwares.eppendorfs, 9)
		reagent(Liquids.primerB, Labwares.eppendorfs, 13)
		reagent(Liquids.polymerase, Labwares.eppendorfs, 17)
		
		well_template.pointer_? = Some(Labwares.eppendorfs.commonObj(B1))
		well_masterMix.pointer_? = Some(Labwares.eppendorfs.commonObj(B2))
		
		labware(plate_balance, Sites.cooled1, LabwareModels.platePcr)
		labware(plate_working, Sites.cooled2, LabwareModels.platePcr)
		for (wells <- well_template.getWells(kb); wellObj <- wells) {
			val wellSetup = kb.getWellSetup(wellObj)
			val sLiquid = "template#"+wellSetup.index_?.get
			val liquid = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
			liquid.setup.sName_? = Some(sLiquid)
			wellSetup.reagent_? = Some(liquid)
		}
		
		station.centrifuge.setup.plate_balance = plate_balance
		setup_thermocycle.program.program = "0,2"
		setup_centrifuge.idProgram_? = Some("2000,15,9,9,20")
	}
}
