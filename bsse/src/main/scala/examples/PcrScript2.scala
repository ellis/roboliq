package examples

import roboliq.common
import roboliq.labs.bsse.Protocol


class PcrScript2(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
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
		val template = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
	}
	
	val well_masterMix = new common.WellPointerVar
	val plate_working = new Plate
	val plate_balance = new Plate
	val wells_working = new common.WellPointerVar
	
	import roboliq.commands.pipette.L4A_PipetteItem
	import roboliq.commands.pipette.MixSpec
	/*mixture(dest = well_masterMix, List(
		(Liquids.buffer10x, 20),
		(Liquids.dNTP, 20),
		(well_template, 1),
		(Liquids.primerF, 1),
		(Liquids.primerB, 1),
		(Liquids.water, 156))
	)
	distribute(well_masterMix, wells_working, 20, premix = MixSpec(Some(200 * 0.75), Some(4)))
	*/
	val items = List(
		new L4A_PipetteItem(Liquids.water, well_masterMix, List(156), None, None),
		new L4A_PipetteItem(Liquids.buffer10x, well_masterMix, List(20), None, None),
		new L4A_PipetteItem(Liquids.dNTP, well_masterMix, List(20), None, None),
		new L4A_PipetteItem(Liquids.template, well_masterMix, List(1), None, None),
		new L4A_PipetteItem(Liquids.primerF, well_masterMix, List(1), None, None),
		new L4A_PipetteItem(Liquids.primerB, well_masterMix, List(1), None, None),
		new L4A_PipetteItem(Liquids.polymerase, well_masterMix, List(1), None, Some(MixSpec(Some(200 * 0.75), Some(4))))/*,
		new L4A_PipetteItem(well_masterMix, wells_working, List(20), Some(MixSpec(Some(200 * 0.75), Some(4))), None)*/
	)
	cmds += roboliq.commands.pipette.L4C_Pipette(new roboliq.commands.pipette.L4A_PipetteArgs(items, tipOverrides_? = None))
	cmds += roboliq.commands.pipette.L4C_Pipette(new roboliq.commands.pipette.L4A_PipetteArgs(Seq(new L4A_PipetteItem(well_masterMix, wells_working, List(20), Some(MixSpec(Some(200 * 0.75), Some(4))), None)), tipOverrides_? = None))
	
	seal(plate_working)
	val setup_thermocycle = thermocycle(plate_working)
	val setup_centrifuge = centrifuge(plate_working)
	//peel(plate_working)

	__findLabels(Liquids)

	val lab = new EvowareLab {
		import station._

		reagent(Liquids.water, Labwares.reagents50, 1)
		reagent(Liquids.buffer10x, Labwares.eppendorfs, 1)
		reagent(Liquids.dNTP, Labwares.eppendorfs, 2)
		reagent(Liquids.template, Labwares.eppendorfs, 5)
		reagent(Liquids.primerF, Labwares.eppendorfs, 6)
		reagent(Liquids.primerB, Labwares.eppendorfs, 7)
		reagent(Liquids.polymerase, Labwares.eppendorfs, 8)
		
		labware(plate_balance, Sites.cooled1, LabwareModels.platePcr)
		labware(plate_working, Sites.cooled2, LabwareModels.platePcr)
		
		well_masterMix.pointer_? = Some(Labwares.eppendorfs.commonObj(A3))
		wells_working.pointer_? = Some(plate_working(D6+2)+plate_working(D7+2))

		station.centrifuge.setup.plate_balance = plate_balance
		setup_thermocycle.program.program = "0,5"
		setup_centrifuge.idProgram_? = Some("2000,15,9,9,20")
	}
}
