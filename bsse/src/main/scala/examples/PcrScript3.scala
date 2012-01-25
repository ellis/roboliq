package examples

import roboliq.common
import roboliq.labs.bsse.Protocol


class PcrScript3(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
	import roboliq.commands.MixItemL4
	import roboliq.commands.MixItemReagentL4
	import roboliq.commands.MixItemTemplateL4
	
	object Liquids {
		val water = new Liquid("Water", CleanPolicy.TNL)
		val buffer10x = new Liquid("Water", CleanPolicy.TNT)
		val dNTP = new Liquid("Water", CleanPolicy.TNT)
		val primerF = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val primerB = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
		val polymerase = new Liquid("Glycerol", CleanPolicy.TNT)
		val template = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
	}
	
	val plate_working = new Plate
	val plate_balance = new Plate
	val well1 = plate_working(A1)
	val well2 = plate_working(B1)
	val well3 = plate_working(C1)
	val well4 = plate_working(D1)
	
	import roboliq.commands.pipette.L4A_PipetteItem
	import roboliq.commands.pipette.MixSpec
	val items = List(
		new L4A_PipetteItem(Liquids.water, well1, List(15.7), None, None),
		new L4A_PipetteItem(Liquids.water, well2, List(15.7), None, None),
		new L4A_PipetteItem(Liquids.water, well3, List(15.7), None, None),
		new L4A_PipetteItem(Liquids.water, well4, List(15.7), None, None),
		new L4A_PipetteItem(Liquids.buffer10x, well1, List(2), None, None),
		new L4A_PipetteItem(Liquids.buffer10x, well2, List(2), None, None),
		new L4A_PipetteItem(Liquids.buffer10x, well3, List(2), None, None),
		new L4A_PipetteItem(Liquids.buffer10x, well4, List(2), None, None),
		new L4A_PipetteItem(Liquids.dNTP, well1, List(2), None, None),
		new L4A_PipetteItem(Liquids.dNTP, well2, List(2), None, None),
		new L4A_PipetteItem(Liquids.dNTP, well3, List(2), None, None),
		new L4A_PipetteItem(Liquids.dNTP, well4, List(2), None, None),
		new L4A_PipetteItem(Liquids.template, well1, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.template, well2, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.template, well3, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.template, well4, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerF, well1, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerF, well2, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerF, well3, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerF, well4, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerB, well1, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerB, well2, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerB, well3, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerB, well4, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.polymerase, well1, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.polymerase, well2, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.polymerase, well3, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.polymerase, well4, List(0.1), None, None)
	)
	/*val items = List(
		new L4A_PipetteItem(Liquids.buffer10x, well1, List(2), None, None),
		new L4A_PipetteItem(Liquids.dNTP, well1, List(2), None, None),
		new L4A_PipetteItem(Liquids.template, well1, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerF, well1, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerB, well1, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerB, well2, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerB, well3, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.primerB, well4, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.polymerase, well1, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.polymerase, well2, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.polymerase, well3, List(0.1), None, None),
		new L4A_PipetteItem(Liquids.polymerase, well4, List(0.1), None, None)
	)*/
	cmds += roboliq.commands.pipette.L4C_Pipette(new roboliq.commands.pipette.L4A_PipetteArgs(items, tipOverrides_? = None))
	mix(well1+well2+well3+well4, 15, 4)
	//cmds += roboliq.commands.pipette.L4C_Pipette(new roboliq.commands.pipette.L4A_PipetteArgs(Seq(new L4A_PipetteItem(well_masterMix, wells_working, List(20), Some(MixSpec(Some(200 * 0.75), Some(4))), None)), tipOverrides_? = None))
	
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
		
		station.centrifuge.setup.plate_balance = plate_balance
		setup_thermocycle.program.program = "0,5"
		setup_centrifuge.idProgram_? = Some("2000,15,9,9,20")
	}
}
