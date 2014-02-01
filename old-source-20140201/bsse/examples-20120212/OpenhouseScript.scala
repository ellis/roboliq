package examples

import roboliq.common
import roboliq.common.WellIndex
import roboliq.commands.pipette._
import roboliq.labs.bsse.Protocol


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
			cmds += L4C_Pipette(new L4A_PipetteArgs(w.map(pair => new L4A_PipetteItem(Liquids.water, plate(WellIndex(pair._2)), Seq(pair._1), None, None))))
		if (!c10.isEmpty)
			cmds += L4C_Pipette(new L4A_PipetteArgs(c10.map(pair => new L4A_PipetteItem(Liquids.color10, plate(WellIndex(pair._2)), Seq(pair._1), None, None))))
		if (!c1.isEmpty)
			cmds += L4C_Pipette(new L4A_PipetteArgs(c1.map(pair => new L4A_PipetteItem(Liquids.color1, plate(WellIndex(pair._2)), Seq(pair._1), None, None))))
	}
	
	def proc1(plate: Plate): Tuple2[roboliq.commands.pcr.PcrThermocycle.Setup, roboliq.commands.centrifuge.L4A_CentrifugeSetup] = {
		val tipOverrides = new roboliq.commands.pipette.TipHandlingOverrides(None, Some(roboliq.common.CleanIntensity.None), None, None)
		pipette(Plates.pretty, plate, 25 ul, tipOverrides)
		seal(plate)
		val setup_thermocycle = thermocycle(plate)
		val setup_centrifuge = centrifuge(plate, Plates.balance)
		(setup_thermocycle, setup_centrifuge)
	}
	
	gradient(Plates.pretty)
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
	object Liquids {
		val water = new Liquid("Water", CleanPolicy.TNT)
	}
	
	object Plates {
		val balance = new Plate
		val pcr = new Plate
	}
		
	def proc1(plate: Plate): Tuple2[roboliq.commands.pcr.PcrThermocycle.Setup, roboliq.commands.centrifuge.L4A_CentrifugeSetup] = {
		val tipOverrides = new roboliq.commands.pipette.TipHandlingOverrides(None, Some(roboliq.common.CleanIntensity.None), None, None)
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

