package roboliq.devices.pcr

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands._
import roboliq.commands.move._
import roboliq.commands.pcr._
import roboliq.commands.pipette._
import roboliq.compiler._


class L3P_PcrMix extends CommandCompilerL3 {
	type CmdType = L3C_PcrMix
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		for {
			iRoma <- device.getRomaId(cmd.args)
		} yield {
			val (lidHandling, locationLid) = cmd.args.lidHandlingSpec_? match {
				case None => (LidHandling.NoLid, "")
				case Some(spec) => spec match {
					case LidRemoveSpec(location) => (LidHandling.RemoveAtSource, location)
					case LidCoverSpec(location) => (LidHandling.CoverAtSource, location)
				}
			}
			val args2 = L2A_MovePlateArgs(
				iRoma, // 0 for RoMa1, 1 for RoMa2
				cmd.args.plate,
				cmd.args.location.value(ctx.states),
				lidHandling,
				locationLid
			)
			Seq(L2C_MovePlate(args2))
		}
	}

	def x(args: L3A_PcrMixArgs) {
		import args._
		//src: WellPointer, well_masterMix: WellPointer, dest: WellPointer, template: Template, components: Seq[MixItemL3], v1: Double) {
		// Calculate desired sample volume for each component
		val components = items.collect { case item: MixItemReagentL3 => item }
		val mapComponentVolumes = components.map(component => {
			val v = component.c1 * v1 / component.c0
			component -> v
		}).toMap
		val vComponentsTotal = mapComponentVolumes.values.reduce(_ + _)
		
		val template = items.collect({ case item: MixItemTemplateL3 => item }).head
		
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
		pipette(water, masterMixWells, vWaterMix * nMult)
		for (component <- components) {
			val vMix = mapComponentVolumes(component) * nMult
			pipette(component.srcs, masterMixWells, vMix)
		}
		// TODO: indicate that liquid in master mix wells is all the same (if more than one well) and label it (eg "MasterMix")
		
		// distribute water to each working well
		val lvWaterPerWell = lvvTemplateWater.map(_._2 - vWaterMinSample)
		pipette(water, dests, lvWaterPerWell)
		
		// distribute template DNA to each working well
		val lvTemplate = lvvTemplateWater.map(_._1)
		pipette(template.srcs, dests, lvTemplate)
		
		// distribute master mix to each working well, free dispense, no wash in-between
		val vMixPerWell = vComponentsTotal + vWaterMix
		pipette(masterMixWells, dests, vMixPerWell)
		
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
	
	def pipette(srcs: Seq[WellConfigL2], dests: Seq[WellConfigL2], lnVolume: Seq[Double]): Seq[Command] = {
		val item = new L4A_PipetteItem(source, dest, lnVolume)
		val cmd = L4C_Pipette(new L4A_PipetteArgs(Seq(item)))
	}

	def pipette(source: WellPointer, dest: WellPointer, volume: Double) = pipette(source, dest, Seq(volume))
}
