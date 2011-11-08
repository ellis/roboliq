package roboliq.devices.pipette.scheduler

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.commands.pipette.{L3A_PipetteItem => Item}


case class GroupA(
	mLM: Map[Item, LM],
	states0: RobotState,
	tipBindings0: Map[TipConfigL2, LM],
	lItem: Seq[Item],
	lLM: Seq[LM],
	mLMToItems: Map[LM, Seq[Item]],
	mLMData: Map[LM, LMData],
	mLMTipCounts: Map[LM, Int],
	mLMToTips: Map[LM, SortedSet[TipConfigL2]],
	mTipToLM: Map[TipConfigL2, LM],
	mDestToTip: Map[Item, TipConfigL2],
	mTipToVolume: Map[TipConfigL2, Double],
	mTipToCleanSpec: Map[TipConfigL2, WashSpec],
	lDispense: Seq[TipWellVolumePolicy],
	lAspirate: Seq[TipWellVolumePolicy],
	bClean: Boolean,
	states1: RobotState
) {
	override def toString: String = {
		List(
			//"mLMToItems:\n"+mLMToItems.toSeq.map(pair => pair._1.toString + " -> " + L3A_PipetteItem.toDebugString(pair._2)).mkString("    ", "\n    ", ""),
			"lItem:\n    "+L3A_PipetteItem.toDebugString(lItem),
			lLM.map(lm => lm.toString + " -> " + mLMData(lm)).mkString("mLMData:\n    ", "\n    ", ""),
			lLM.map(lm => lm.toString + " -> " + mLMTipCounts(lm)).mkString("mLMTipCounts:\n    ", "\n    ", ""),
			lLM.map(lm => lm.toString + " -> " + mLMToTips(lm)).mkString("mLMToTips:\n    ", "\n    ", ""),
			//lItem.map(item => Command.getWellsDebugString(Seq(item.dest)) + " -> " + mDestToTip(item)).mkString("mDestToTip:\n    ", "\n    ", ""),
			"mTipToWashIntensity:\n    "+mTipToCleanSpec.mapValues(_.washIntensity),
			lDispense.map(twvpString).mkString("lDispense:\n    ", "\n    ", ""),
			lAspirate.map(twvpString).mkString("lAspirate:\n    ", "\n    ", "")
		).mkString("GroupZ(\n  ", "\n  ", ")\n")
	}
	
	private def twvpString(twvp: TipWellVolumePolicy): String = {
		List(twvp.tip, Command.getWellsDebugString(Seq(twvp.well)), twvp.nVolume, twvp.policy.id).mkString(", ")			
	}
}
