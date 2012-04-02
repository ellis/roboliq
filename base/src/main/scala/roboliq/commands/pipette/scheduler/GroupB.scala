package roboliq.commands.pipette.scheduler

import scala.collection.immutable.SortedSet

import roboliq.core._
import roboliq.commands.pipette._


case class GroupB(
	lItem: Seq[Item],
	bClean: Boolean,
	precleans: Map[Tip, CleanSpec2],
	cleans: Map[Tip, CleanSpec2],
	lTipCleanable: SortedSet[Tip],
	premixes: Seq[TipWellVolume],
	lPremix: Seq[MixCmdBean],
	lAspirate: Seq[AspirateCmdBean],
	lDispense: Seq[DispenseCmdBean],
	lPostmix: Seq[MixCmdBean],
	nScore: Double
) {
	override def toString: String = {
		List(
			//"mLMToItems:\n"+mLMToItems.toSeq.map(pair => pair._1.toString + " -> " + L3A_PipetteItem.toDebugString(pair._2)).mkString("    ", "\n    ", ""),
			"lItem:\n    "+lItem.toString,
			precleans.map(_.toString).mkString("precleans:\n    ", "\n    ", ""),
			cleans.map(_.toString).mkString("cleans:\n    ", "\n    ", ""),
			"lTipCleanable:\n    "+lTipCleanable,
			lPremix.map(_.toString).mkString("lPremix:\n    ", "\n    ", ""),
			lAspirate.map(_.toString).mkString("lAspirate:\n    ", "\n    ", ""),
			lDispense.map(_.toString).mkString("lDispense:\n    ", "\n    ", ""),
			lPostmix.map(_.toString).mkString("lPostmix:\n    ", "\n    ", ""),
			"nScore:\n    "+nScore
		).mkString("GroupB(\n  ", "\n  ", ")\n")
	}
}
