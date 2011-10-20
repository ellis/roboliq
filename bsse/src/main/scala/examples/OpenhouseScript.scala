package examples

import roboliq.common
import roboliq.common.WellIndex
import roboliq.commands.pipette._
import roboliq.labs.bsse.Protocol

/* Things to improve:
 * clear plates
 * 0 color to begin with
 * fill the wells more so that they are more visible
 * move RoMa1 back home after RoMa2 moves plate from sealer back home
 */
class ExampleOpenhouse(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
	import roboliq.commands.MixItemL4
	import roboliq.commands.MixItemReagentL4
	import roboliq.commands.MixItemTemplateL4
	
	object Liquids {
		val water = new Liquid("Water", CleanPolicy.TNT)
		val color = new Liquid("Water", CleanPolicy.TNT)
	}
	
	val plate_balance = new Plate
	val plate1 = new Plate
	//val plate2 = new Plate
	
	val nWellVolume = 50.0
	
	// Fill the balance plate
	pipette(Liquids.water, plate_balance, nWellVolume)
	seal(plate_balance)
	
	def proc1(plate: Plate): Tuple2[roboliq.commands.pcr.PcrThermocycle.Setup, roboliq.commands.centrifuge.L4A_CentrifugeSetup] = {
		cmds += L4C_Pipette(new L4A_PipetteArgs(for (i <- 0 until 96) yield {
			val nVolume = 5 + (nWellVolume - 5) * (95.0 - i) / 95.0;
			new L4A_PipetteItem(Liquids.water, plate(WellIndex(i)), Seq(nVolume))
		}))
		cmds += L4C_Pipette(new L4A_PipetteArgs(for (i <- 0 until 96) yield {
			val nVolume = 5 + (nWellVolume - 5) * i / 95.0;
			new L4A_PipetteItem(Liquids.color, plate(WellIndex(i)), Seq(nVolume))
		}))
		
		seal(plate)
		val setup_thermocycle = thermocycle(plate)
		val setup_centrifuge = centrifuge(plate, plate_balance)
		(setup_thermocycle, setup_centrifuge)
	}
	
	val (t1, c1) = proc1(plate1)

	__findLabels(Liquids)

	val lab = new EvowareLab {
		import station._

		reagent(Liquids.water, Labwares.reagents50, 1)
		reagent(Liquids.color, Labwares.reagents15, 1)
		
		labware(plate_balance, Sites.cooled5, LabwareModels.platePcr)
		labware(plate1, Sites.cooled1, LabwareModels.platePcr)
		
		t1.program.program = "<NONE>"
		c1.idProgram_? = Some("2000,15,9,9,20")
	}
}
