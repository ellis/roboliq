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

class ExampleOpenhouse2(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
	import roboliq.commands.MixItemL4
	import roboliq.commands.MixItemReagentL4
	import roboliq.commands.MixItemTemplateL4
	
	object Liquids {
		val water = new Liquid("Water", CleanPolicy.TNT)
		val color1 = new Liquid("Water", CleanPolicy.TNT)
		val color10 = new Liquid("Water", CleanPolicy.TNT) // Diluted by 10
	}
	
	object Plates {
		val balance = new Plate
		val pcr = new Plate
		val pretty = new Plate
	}
		
	def gradient(plate: Plate) {
		val nWellVolume = 100.0
		
		val wcc: Seq[Tuple3[Double, Double, Double]] = (0 until 96).map(i => {
			val nColorVolume = nWellVolume * i / 95
			val wcc: Tuple3[Double, Double, Double] = {
				if (nColorVolume * 10 < 5)
					(nWellVolume, 0, 0)
				else if (nColorVolume < 5) {
					val nColor10Volume = nColorVolume * 10
					(nWellVolume - nColor10Volume, nColor10Volume, 0)
				}
				else if (nColorVolume <= nWellVolume - 5)
					(nWellVolume - nColorVolume, 0, nColorVolume)
				else
					(0, 0, nWellVolume)
			}
			wcc
		})
		val w = wcc.map(_._1).zipWithIndex.filter(_._1 > 0)
		val c10 = wcc.map(_._2).zipWithIndex.filter(_._1 > 0)
		val c1 = wcc.map(_._3).zipWithIndex.filter(_._1 > 0)
		if (!w.isEmpty)
			cmds += L4C_Pipette(new L4A_PipetteArgs(w.map(pair => new L4A_PipetteItem(Liquids.water, plate(WellIndex(pair._2)), Seq(pair._1)))))
		if (!c10.isEmpty)
			cmds += L4C_Pipette(new L4A_PipetteArgs(c10.map(pair => new L4A_PipetteItem(Liquids.color10, plate(WellIndex(pair._2)), Seq(pair._1)))))
		if (!c1.isEmpty)
			cmds += L4C_Pipette(new L4A_PipetteArgs(c1.map(pair => new L4A_PipetteItem(Liquids.color1, plate(WellIndex(pair._2)), Seq(pair._1)))))
	}
	
	def proc1(plate: Plate): Tuple2[roboliq.commands.pcr.PcrThermocycle.Setup, roboliq.commands.centrifuge.L4A_CentrifugeSetup] = {
		val tipOverrides = new roboliq.commands.pipette.TipHandlingOverrides(None, Some(roboliq.common.WashIntensity.None), None, None)
		pipette(Plates.pretty, plate, 25 ul, tipOverrides)
		seal(plate)
		val setup_thermocycle = thermocycle(plate)
		val setup_centrifuge = centrifuge(plate, Plates.balance)
		(setup_thermocycle, setup_centrifuge)
	}
	
	gradient(Plates.pretty)
	//pipette(Liquids.water, Plates.balance, 25 ul)
	//seal(Plates.balance)
	val (t1, c1) = proc1(Plates.pcr)

	__findLabels(Liquids)
	__findLabels(Plates)

	val lab = new EvowareLab {
		import station._

		reagent(Liquids.water, Labwares.reagents50, 1, 2)
		reagent(Liquids.color10, Labwares.reagents15, 1, 2)
		reagent(Liquids.color1, Labwares.reagents15, 3, 2)
		
		labware(Plates.pretty, Sites.cover, LabwareModels.plateCostar)
		labware(Plates.pcr, Sites.cooled1, LabwareModels.platePcr)
		labware(Plates.balance, Sites.cooled2, LabwareModels.platePcr)
		
		t1.program.program = "<NONE>"
		c1.idProgram_? = Some("2000,15,9,9,20")
	}
}

class ExampleOpenhouse3(station: roboliq.labs.bsse.station1.StationConfig) extends Protocol {
	import roboliq.commands.MixItemL4
	import roboliq.commands.MixItemReagentL4
	import roboliq.commands.MixItemTemplateL4
	
	object Liquids {
		val water = new Liquid("Water", CleanPolicy.TNT)
	}
	
	object Plates {
		val balance = new Plate
		val pcr = new Plate
	}
		
	def proc1(plate: Plate): Tuple2[roboliq.commands.pcr.PcrThermocycle.Setup, roboliq.commands.centrifuge.L4A_CentrifugeSetup] = {
		val tipOverrides = new roboliq.commands.pipette.TipHandlingOverrides(None, Some(roboliq.common.WashIntensity.None), None, None)
		pipette(Liquids.water, plate, 25 ul, tipOverrides)
		seal(plate)
		val setup_thermocycle = thermocycle(plate)
		val setup_centrifuge = centrifuge(plate, Plates.balance)
		(setup_thermocycle, setup_centrifuge)
	}
	
	val (t1, c1) = proc1(Plates.pcr)

	__findLabels(Liquids)
	__findLabels(Plates)

	val lab = new EvowareLab {
		import station._

		reagent(Liquids.water, Labwares.reagents50, 1, 2)
		
		labware(Plates.pcr, Sites.cooled1, LabwareModels.platePcr)
		labware(Plates.balance, Sites.cooled2, LabwareModels.platePcr)
		
		t1.program.program = "<NONE>"
		c1.idProgram_? = Some("2000,15,9,9,20")
	}
}

