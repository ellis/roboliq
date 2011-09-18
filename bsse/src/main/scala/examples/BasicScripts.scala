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
	
	object Liquids {
		val water = new Liquid("Water")
		val buffer10x = new Liquid("Water")
		val dNTP = new Liquid("Water")
		val primerF = new Liquid("Water")
		val primerB = new Liquid("Water")
		val polymerase = new Liquid("Glycerol")
	}
	
	val plate_template = new Plate
	val plate_working = new Plate
	
	case class Template(lc0: Seq[Double], c1: Double, vMin: Double = 0, vMax: Double = Double.MaxValue)
	case class Component(liquid: Liquid, c0: Double, c1: Double, vMin: Double = 0, vMax: Double = Double.MaxValue)
	
	val seq = Seq[Component](
		Component(Liquids.buffer10x, 10, 1),
		Component(Liquids.dNTP, 2.5, .2),
		Component(Liquids.primerF, 50, 8),
		Component(Liquids.primerB, 50, 8),
		Component(Liquids.polymerase, 5, 1.25/50)
	)
	
	def x(template: Template, components: Seq[Component], v1: Double) {
		val water = Liquids.water
		val mapLiquidToVolume = seq.map(component => {
			val v = component.c1 * v1 / component.c0
			component.liquid -> v
		}).toMap
		val vComponents = mapLiquidToVolume.values.reduce((v1, v2) => v1 + v2)
		components.foreach(component => println(component.liquid.toString+": "+mapLiquidToVolume(component.liquid)))
		template.lc0.zipWithIndex.foreach(pair => {
			val (c0, i) = pair
			val vTemplate = template.c1 * v1 / c0
			val vWater = v1 - vComponents - vTemplate
			println(i+": "+vTemplate+" + "+vWater+" water")
		})
		//println(mapLiquidToVolume)
	}
	
	__findLabels(Liquids)
	x(Template(Seq(20), 0.2), seq, 50 ul)

	val lab = new EvowareLab {
		import station._

		reagent(Liquids.water, Labwares.reagents50, 1, 8)
		reagent(Liquids.buffer10x, Labwares.eppendorfs, 1)
		reagent(Liquids.dNTP, Labwares.eppendorfs, 5)
		reagent(Liquids.primerF, Labwares.eppendorfs, 9)
		reagent(Liquids.primerB, Labwares.eppendorfs, 13)
		reagent(Liquids.polymerase, Labwares.eppendorfs, 17)
		
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
