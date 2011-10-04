package roboliq.commands.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._


trait PipetteCommands extends RoboliqCommands {
	def mix(target: WellPointer, volume: Double, count: Int) {
		val mixSpec = new MixSpec(volume, count)
		val args = new L4A_MixArgs(Seq(target), mixSpec)
		val cmd = L4C_Mix(args)
		cmds += cmd
	}
	
	def pipette(source: WellPointer, dest: WellPointer, volume: Double) {
		pipette(source, dest, Seq(volume))
	}
	
	def pipette(source: WellPointer, dest: WellPointer, lnVolume: Seq[Double]) {
		val item = new L4A_PipetteItem(source, dest, lnVolume)
		val cmd = L4C_Pipette(new L4A_PipetteArgs(Seq(item)))
		cmds += cmd
	}
}

object PipetteCommandsL3 {
	def pipette(states: RobotState, srcs: Seq[WellConfigL2], dests: Seq[WellConfigL2], lnVolume: Seq[Double]): Result[L3C_Pipette] = {
		val lLiquid = srcs.map(_.state(states).liquid).toSet
		for {
			_ <- Result.assert(lLiquid.size == 1 || srcs.size == dests.size, "you must specify an equal number of source and destination wells: "+srcs+" vs "+dests)
			_ <- Result.assert(lnVolume.size == 1 || dests.size == lnVolume.size, "you must specify an equal number of destinations and volumes: "+dests+" vs "+lnVolume)
		} yield {
			val mapDestToVolume = {
				if (lnVolume.size == 1) dests.map(_ -> lnVolume.head).toMap
				else (dests zip lnVolume).toMap
			}
			val items3 = {
				if (lLiquid.size == 1)
					dests.map(dest => new L3A_PipetteItem(SortedSet(srcs : _*), dest, mapDestToVolume(dest)))
				else
					(srcs.toSeq zip dests.toSeq).map(pair => new L3A_PipetteItem(SortedSet(pair._1), pair._2, mapDestToVolume(pair._2)))
			}
			val args = new L3A_PipetteArgs(items3)
			L3C_Pipette(args)
		}
	}

	def pipette(states: RobotState, srcs: Seq[WellConfigL2], dests: Seq[WellConfigL2], nVolume: Double): Result[L3C_Pipette] = {
		pipette(states, srcs, dests, Seq(nVolume))
	}
}
