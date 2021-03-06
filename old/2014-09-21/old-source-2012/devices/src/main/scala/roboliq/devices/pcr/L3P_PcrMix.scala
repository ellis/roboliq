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
		x(ctx.states, cmd.args)
	}

	def x(states: RobotState, args: L3A_PcrMixArgs): Result[Seq[Command]] = {
		import args._
		//src: WellPointer, well_masterMix: WellPointer, dest: WellPointer, template: Template, components: Seq[MixItemL3], v1: Double) {
		// Calculate desired sample volume for each component
		val components = items.collect { case item: MixItemReagentL3 => item }
		val mapComponentVolumes = components.map(component => {
			val v = component.c1 * v1 / component.c0
			component -> v
		}).toMap
		val lvComponent = mapComponentVolumes.values
		val vComponentsTotal = lvComponent.reduce(_ + _)
		
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
		val nSamples = dests.size
		val nMult: Int = nSamples + 1
		def volForMix(vSample: Double): Double = { vSample * nMult }
		
		val vWaterMinSample = {
			val lvWater2 = lvvTemplateWater.map(_._2).toSet[Double].toSeq.sortBy(identity)
			// Smallest volume of water in a sample
			// (adjusted to ensure a minimal difference to the next lowest volume)
			lvWater2.toList match {
				case Nil => return Error("INTERNAL: PcrMix error 1")
				case vMin :: Nil => vMin
				case vMin :: vMin1 :: _ => if (vMin > vMin1 - vLowerBound) vMin - vLowerBound else vMin
			}
		}

		val vWaterMix = volForMix(vWaterMinSample)
		val lvWaterPerWell = lvvTemplateWater.map(_._2 - vWaterMinSample)

		/*
		for {
			// distribute water to each working well
			cmd1 <- PipetteCommandsL3.pipette(states, water, masterMixWells, vWaterMix * nMult)
			// create master mix
			cmds2 <- Result.mapOver(components)(component => {
				val vMix = mapComponentVolumes(component) * nMult
				PipetteCommandsL3.pipette(states, component.srcs, masterMixWells, vMix)
			})
			// TODO: indicate that liquid in master mix wells is all the same (if more than one well) and label it (eg "MasterMix")
			cmd3 <- PipetteCommandsL3.pipette(states, water, dests, lvWaterPerWell)
			// distribute template DNA to each working well
			val lvTemplate = lvvTemplateWater.map(_._1)
			cmd4 <- PipetteCommandsL3.pipette(states, template.srcs, dests, lvTemplate)
			// Mix the master mix before distributing it
			val vMasterMixWell = (vComponentsTotal + vWaterMinSample) * nMult
			cmd5 <- PipetteCommandsL3.mix(states, masterMixWells, vMasterMixWell * 0.75, 4)
			// distribute master mix to each working well, free dispense, no wash in-between
			val vMixPerWell = vComponentsTotal + vWaterMinSample
			cmd6 <- PipetteCommandsL3.pipette(states, masterMixWells, dests, vMixPerWell)
		} yield {
			println("lvComponent: "+lvComponent)
			println("vComponentsTotal: "+vComponentsTotal)
			println("vWaterMinSample: "+vWaterMinSample)
			println("vWaterMix: "+vWaterMix)
			println("lvWaterPerWell: "+lvWaterPerWell)
			Seq(cmd1) ++ cmds2 ++ Seq(cmd3, cmd4, cmd5, cmd6)
		}
		*/
		for {
			// distribute water to each working well
			cmd1 <- PipetteCommandsL3.pipette(states, water, masterMixWells, vWaterMix * nMult)
			// create master mix
			items2 <- Result.flatMap(components)(component => {
				val vMix = mapComponentVolumes(component) * nMult
				PipetteCommandsL3.pipetteItems(states, component.srcs, masterMixWells, vMix)
			})
			// TODO: indicate that liquid in master mix wells is all the same (if more than one well) and label it (eg "MasterMix")
			cmd3 <- PipetteCommandsL3.pipette(states, water, dests, lvWaterPerWell)
			// distribute template DNA to each working well
			val lvTemplate = lvvTemplateWater.map(_._1)
			cmd4 <- PipetteCommandsL3.pipette(states, template.srcs, dests, lvTemplate)
			// Mix the master mix before distributing it
			val vMasterMixWell = (vComponentsTotal + vWaterMinSample) * nMult
			cmd5 <- PipetteCommandsL3.mix(states, masterMixWells, vMasterMixWell * 0.75, 4)
			// distribute master mix to each working well, free dispense, no wash in-between
			val vMixPerWell = vComponentsTotal + vWaterMinSample
			cmd6 <- PipetteCommandsL3.pipette(states, masterMixWells, dests, vMixPerWell)
		} yield {
			println("lvComponent: "+lvComponent)
			println("vComponentsTotal: "+vComponentsTotal)
			println("vWaterMinSample: "+vWaterMinSample)
			println("vWaterMix: "+vWaterMix)
			println("lvWaterPerWell: "+lvWaterPerWell)
			val items: Seq[L3A_PipetteItem] = cmd1.args.items ++ items2 ++ cmd3.args.items ++ cmd4.args.items
			Seq(L3C_Pipette(new L3A_PipetteArgs(items)), cmd5, cmd6)
		}
		
			
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
}
