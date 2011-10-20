package examples

import roboliq.common
import roboliq.common.WellIndex
import roboliq.commands.pipette._
import roboliq.labs.bsse.Protocol


class ExampleOpenhouse(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
	import roboliq.commands.MixItemL4
	import roboliq.commands.MixItemReagentL4
	import roboliq.commands.MixItemTemplateL4
	
	object Liquids {
		val water = new Liquid("Water", CleanPolicy.TNT)
		val color = new Liquid("Glycerol", CleanPolicy.TNT)
	}
	
	val plate1 = new Plate
	val plate2 = new Plate
	
	val nWellVolume = 50.0;
	cmds += L4C_Pipette(new L4A_PipetteArgs(for (i <- 0 until 96) yield {
		val nVolume = 5 + (nWellVolume - 5) * (95.0 - i) / 95.0;
		new L4A_PipetteItem(Liquids.water, plate1(WellIndex(i)), Seq(nVolume))
	}))
	cmds += L4C_Pipette(new L4A_PipetteArgs(for (i <- 0 until 96) yield {
		val nVolume = 5 + (nWellVolume - 5) * i / 95.0;
		new L4A_PipetteItem(Liquids.color, plate1(WellIndex(i)), Seq(nVolume))
	}))
	pipette(Liquids.water, plate2, nWellVolume)
	
	seal(plate1)
	seal(plate2)
	val setup_thermocycle = thermocycle(plate1)
	val setup_centrifuge = centrifuge(plate1, plate2)

	__findLabels(Liquids)

	val lab = new EvowareLab {
		import station._

		reagent(Liquids.water, Labwares.reagents50, 1, 4)
		reagent(Liquids.color, Labwares.reagents50, 5, 4)
		
		labware(plate1, Sites.cooled1, LabwareModels.platePcr)
		labware(plate2, Sites.cooled2, LabwareModels.platePcr)
		
		setup_thermocycle.program.program = "<NONE>"
		setup_centrifuge.idProgram_? = Some("2000,15,9,9,20")
	}
}
