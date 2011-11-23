package examples

import roboliq.common
import roboliq.labs.bsse.Protocol


class PcrScript1(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
	import roboliq.commands.MixItemL4
	import roboliq.commands.MixItemReagentL4
	import roboliq.commands.MixItemTemplateL4
	
	object Liquids {
		val water = new Liquid("Water", CleanPolicy.TNN)
		val buffer10x = new Liquid("Water", CleanPolicy.TNT)
		val dNTP = new Liquid("Water", CleanPolicy.TNT)
		val primerF = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerB = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val polymerase = new Liquid("Glycerol", CleanPolicy.TNT)
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
	
	pcrMix(plate_working(C6+4), mixItems, Liquids.water, 50 ul, well_masterMix)
	seal(plate_working)
	val setup_thermocycle = thermocycle(plate_working)
	val setup_centrifuge = centrifuge(plate_working)
	peel(plate_working)

	__findLabels(Liquids)

	val lab = new EvowareLab {
		import station._

		reagent(Liquids.water, Labwares.reagents50, 1)
		reagent(Liquids.buffer10x, Labwares.eppendorfs, 1)
		reagent(Liquids.dNTP, Labwares.eppendorfs, 2)
		reagent(Liquids.primerF, Labwares.eppendorfs, 3)
		reagent(Liquids.primerB, Labwares.eppendorfs, 4)
		reagent(Liquids.polymerase, Labwares.eppendorfs, 5)
		
		well_template.pointer_? = Some(Labwares.eppendorfs.commonObj(D2))
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
