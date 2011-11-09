package roboliq.devices.pipette.scheduler

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.commands.pipette.{L3A_PipetteItem => Item}

/*
case class GroupC(
	cleans: Map[TipConfigL2, CleanSpec2],
	premixes: Seq[TipWellVolume],
	lAspirate: Seq[L2C_Aspirate],
	lDispense: Seq[L2C_Dispense],
	postmixes: Seq[TipWellVolume]
) {
	override def toString: String = {
		List(
			//"mLMToItems:\n"+mLMToItems.toSeq.map(pair => pair._1.toString + " -> " + L3A_PipetteItem.toDebugString(pair._2)).mkString("    ", "\n    ", ""),
			precleans.map(_.toString).mkString("precleans:\n    ", "\n    ", ""),
			cleans.map(_.toString).mkString("cleans:\n    ", "\n    ", ""),
			"lTipCleanable:\n    "+lTipCleanable,
			lAspirate.map(_.toDebugString).mkString("lAspirate:\n    ", "\n    ", ""),
			lDispense.map(_.toDebugString).mkString("lDispense:\n    ", "\n    ", ""),
			"nScore:\n    "+nScore
		).mkString("GroupB(\n  ", "\n  ", ")\n")
	}
}
*/