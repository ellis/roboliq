package roboliq.commands.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._


trait PipetteCommands extends RoboliqCommands {
	def mix(target: WellPointer, volume: Double, count: Int) {
		val mixSpec = new MixSpec(Some(volume), Some(count))
		val args = new L4A_MixArgs(Seq(target), mixSpec)
		val cmd = L4C_Mix(args)
		cmds += cmd
	}
	
	def pipette(source: WellPointer, dest: WellPointer, volume: Double) {
		pipette(source, dest, Seq(volume), None)
	}
	
	def pipette(source: WellPointer, dest: WellPointer, volume: Double, tipOverrides: TipHandlingOverrides) {
		pipette(source, dest, Seq(volume), Some(tipOverrides))
	}
	
	def pipette(source: WellPointer, dest: WellPointer, lnVolume: Seq[Double], tipOverrides_? : Option[TipHandlingOverrides] = None) {
		val item = new L4A_PipetteItem(source, dest, lnVolume, None, None)
		val cmd = L4C_Pipette(new L4A_PipetteArgs(Seq(item), tipOverrides_? = tipOverrides_?))
		cmds += cmd
	}
}

object PipetteCommandsL4 {
	def pipette(source: WellPointer, dest: WellPointer, volume: Double): Result[L4C_Pipette] = {
		pipette(source, dest, Seq(volume), None)
	}
	
	def pipette(source: WellPointer, dest: WellPointer, volume: Double, tipOverrides: TipHandlingOverrides): Result[L4C_Pipette] = {
		pipette(source, dest, Seq(volume), Some(tipOverrides))
	}
	
	def pipette(
		source: WellPointer,
		dest: WellPointer,
		lnVolume: Seq[Double]
	): Result[L4C_Pipette] = {
		pipette(source, dest, lnVolume, None)
	}
	
	def pipette(
		source: WellPointer,
		dest: WellPointer,
		lnVolume: Seq[Double],
		tipOverrides_? : Option[TipHandlingOverrides]
	): Result[L4C_Pipette] = {
		pipette(Seq(new L4A_PipetteItem(source, dest, lnVolume, None, None)), tipOverrides_?)
	}
	
	def pipette(
		items: Seq[L4A_PipetteItem],
		tipOverrides_? : Option[TipHandlingOverrides] = None
	): Result[L4C_Pipette] = {
		Success(L4C_Pipette(new L4A_PipetteArgs(items, tipOverrides_? = tipOverrides_?)))
	}
	
	/*
	def copy(source: WellPointer, dest: WellPointer, nVolume: Double, tipOverrides_? : Option[TipHandlingOverrides]): Result[L4C_Pipette] = {
		pipette(Seq(new L4A_PipetteItem(source, dest, Seq(nVolume), None, None)), tipOverrides_?)
	}
	
	def copyWithDilution(
		diluter: WellPointer,
		nVolumeDiluter: Double,
		source: WellPointer,
		nVolumeSrc: Double,
		dest: WellPointer,
		tipOverrides_? : Option[TipHandlingOverrides]
	): Result[Seq[L4C_Pipette]] = {
		for {
			cmd1 <- pipette(Seq(new L4A_PipetteItem(diluter, dest, Seq(nVolumeDiluter), bDuplicate = true)), tipOverrides_?)
			cmd2 <- pipette(Seq(new L4A_PipetteItem(source, dest, Seq(nVolumeSrc), bDuplicate = true)), tipOverrides_?)
		} yield {
			Seq(cmd1, cmd2)
		}
	}*/
}

object PipetteCommandsL3 {
	def mix(states: RobotState, targets: Seq[WellConfigL2], volume: Double, count: Int): Result[L3C_Mix] = {
		val mixSpec = new MixSpec(Some(volume), Some(count))
		val args3 = new L3A_MixArgs(
			targets.map(well => new L3A_MixItem(well, mixSpec)),
			tipOverrides_? = None,
			tipModel_? = None
		)
		Success(L3C_Mix(args3))
	}
	
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
					dests.map(dest => new L3A_PipetteItem(SortedSet(srcs : _*), dest, mapDestToVolume(dest), None, None))
				else
					(srcs.toSeq zip dests.toSeq).map(pair => new L3A_PipetteItem(SortedSet(pair._1), pair._2, mapDestToVolume(pair._2), None, None))
			}
			val args = new L3A_PipetteArgs(items3)
			L3C_Pipette(args)
		}
	}
	
	def pipette(states: RobotState, srcs: Seq[WellConfigL2], dests: Seq[WellConfigL2], nVolume: Double): Result[L3C_Pipette] = {
		pipette(states, srcs, dests, Seq(nVolume))
	}
	
	def pipetteItems(states: RobotState, srcs: Seq[WellConfigL2], dests: Seq[WellConfigL2], lnVolume: Seq[Double]): Result[Seq[L3A_PipetteItem]] = {
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
					dests.map(dest => new L3A_PipetteItem(SortedSet(srcs : _*), dest, mapDestToVolume(dest), None, None))
				else
					(srcs.toSeq zip dests.toSeq).map(pair => new L3A_PipetteItem(SortedSet(pair._1), pair._2, mapDestToVolume(pair._2), None, None))
			}
			items3
		}
	}

	def pipetteItems(states: RobotState, srcs: Seq[WellConfigL2], dests: Seq[WellConfigL2], nVolume: Double): Result[Seq[L3A_PipetteItem]] = {
		pipetteItems(states, srcs, dests, Seq(nVolume))
	}
}
