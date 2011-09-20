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
	import common.WellPointer
	
	object Liquids {
		val water = new Liquid("Water")
		val buffer10x = new Liquid("Water")
		val dNTP = new Liquid("Water")
		val primerF = new Liquid("Water")
		val primerB = new Liquid("Water")
		val polymerase = new Liquid("Glycerol")
	}
	
	val well_template = new common.WellPointerVar
	val well_masterMix = new common.WellPointerVar
	val plate_working = new Plate
	
	case class Template(lc0: Seq[Double], c1: Double, vMin: Double = 0, vMax: Double = Double.MaxValue)
	case class Component(liquid: Liquid, c0: Double, c1: Double, vMin: Double = 0, vMax: Double = Double.MaxValue)
	
	val seq = Seq[Component](
		Component(Liquids.buffer10x, 10, 1),
		Component(Liquids.dNTP, 2, .2),
		Component(Liquids.primerF, 50, .5),
		Component(Liquids.primerB, 50, .5),
		Component(Liquids.polymerase, 5, 0.25/25)
	)
	
	// Steps:
	// create master mix in 15ml well
	// distribute water to each working well
	// distribute template DNA to each working well
	// distribute master mix to each working well, free dispense, no wash in-between
	
	def x(src: WellPointer, well_masterMix: WellPointer, dest: WellPointer, template: Template, components: Seq[Component], v1: Double) {
		val water = Liquids.water
		
		// Calculate desired sample volume for each component
		val mapComponentVolumes = components.map(component => {
			val v = component.c1 * v1 / component.c0
			component -> v
		}).toMap
		val vComponentsTotal = mapComponentVolumes.values.reduce(_ + _)
		
		// Calculate desired sample volume for each template well
		// Calculate volume of water required for each working well in order to reach the required volume
		val lvvTemplateWater = template.lc0.map(c0 => {
			val vTemplate = template.c1 * v1 / c0
			val vWater = v1 - vComponentsTotal - vTemplate
			(vTemplate, vWater)
		})
		
		val vLowerBound = 0.1
		val vExtra = 0//5
		val nSamples = template.lc0.size
		val nMult: Int = nSamples + 1
		def volForMix(vSample: Double): Double = { vSample * nMult }
		
		val vWaterMinSample = {
			val lvWater2 = lvvTemplateWater.map(_._2).toSet[Double].toSeq.sortBy(identity).take(2)
			// Smallest volume of water in a sample
			// (adjusted to ensure a minimal difference to the next lowest volume)
			lvWater2.toList match {
				case vMin :: Nil => vMin
				case vMin :: vMin1 :: Nil => if (vMin > vMin1 - vLowerBound) vMin - vLowerBound else vMin
			}
		}
		val vWaterMix = volForMix(vWaterMinSample)
		
		// create master mix in 15ml well
		pipette(water, well_masterMix, vWaterMix * nMult)
		for (component <- components) {
			val vMix = mapComponentVolumes(component) * nMult
			pipette(component.liquid, well_masterMix, vMix)
		}
		// TODO: indicate that liquid in master mix wells is all the same (if more than one well) and label it (eg "MasterMix")
		
		// distribute water to each working well
		val lvWaterPerWell = lvvTemplateWater.map(_._2 - vWaterMinSample)
		pipette(water, dest, lvWaterPerWell)
		
		// distribute template DNA to each working well
		val lvTemplate = lvvTemplateWater.map(_._1)
		pipette(src, dest, lvTemplate)
		
		// distribute master mix to each working well, free dispense, no wash in-between
		val vMixPerWell = vComponentsTotal + vWaterMix
		pipette(well_masterMix, dest, vMixPerWell)
		
		/*
		components.foreach(component => println(component.liquid.toString+": "+mapLiquidToVolume(component.liquid)))
		template.lc0.zipWithIndex.foreach(pair => {
			val (c0, i) = pair
			val vTemplate = template.c1 * v1 / c0
			val vWater = v1 - vComponentsTotal - vTemplate
			println(i+": "+vTemplate+" + "+vWater+" water")
		})
		*/
	}
	
	__findLabels(Liquids)
	x(well_template, well_masterMix, plate_working(B5+2), Template(Seq(20), 0.2), seq, 50 ul)

	val lab = new EvowareLab {
		import station._

		reagent(Liquids.water, Labwares.reagents50, 1, 8)
		reagent(Liquids.buffer10x, Labwares.eppendorfs, 1)
		reagent(Liquids.dNTP, Labwares.eppendorfs, 5)
		reagent(Liquids.primerF, Labwares.eppendorfs, 9)
		reagent(Liquids.primerB, Labwares.eppendorfs, 13)
		reagent(Liquids.polymerase, Labwares.eppendorfs, 17)
		
		well_template.pointer_? = Some(Labwares.eppendorfs.commonObj(B1))
		well_masterMix.pointer_? = Some(Labwares.reagents15.commonObj(A1))
		
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
