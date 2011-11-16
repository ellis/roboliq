package roboliq.devices.pipette.scheduler

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.commands.pipette.{L3A_PipetteItem => Item}


/**
 * @param mLM map from ALL items (not just the items in this group) to the chosen LM for the destination well
 * @param mTipToLM map from tip to source LM
 * @param mItemToTip map from item to tip used for that item 
 */
case class GroupA(
	mLM: Map[Item, LM],
	states0: RobotState,
	tipBindings0: Map[TipConfigL2, LM],
	mTipToCleanSpecPending0: Map[TipConfigL2, WashSpec],
	lItem: Seq[Item],
	lLM: Seq[LM],
	mLMToItems: Map[LM, Seq[Item]],
	mLMData: Map[LM, LMData],
	mLMTipCounts: Map[LM, Int],
	mLMToTips: Map[LM, SortedSet[TipConfigL2]],
	mTipToLM: Map[TipConfigL2, LM],
	mItemToTip: Map[Item, TipConfigL2],
	mTipToVolume: Map[TipConfigL2, Double],
	mItemToPolicy: Map[Item, PipettePolicy],
	mTipToCleanSpecA: Map[TipConfigL2, WashSpec],
	mTipToCleanSpecPendingA: Map[TipConfigL2, WashSpec],
	mTipToCleanSpec: Map[TipConfigL2, WashSpec],
	mTipToCleanSpecPending: Map[TipConfigL2, WashSpec],
	lDispense: Seq[TipWellVolumePolicy],
	lAspirate: Seq[TipWellVolumePolicy],
	bClean: Boolean,
	states1: RobotState
) {
	override def toString: String = {
		List(
			"lItem:\n    "+L3A_PipetteItem.toDebugString(lItem),
			"mLMToItems:\n"+mLMToItems.toSeq.map(pair => pair._1.toString + " -> " + L3A_PipetteItem.toDebugString(pair._2)).mkString("    ", "\n    ", ""),
			lLM.map(lm => lm.toString + " -> " + mLMData(lm)).mkString("mLMData:\n    ", "\n    ", ""),
			lLM.map(lm => lm.toString + " -> " + mLMTipCounts(lm)).mkString("mLMTipCounts:\n    ", "\n    ", ""),
			lLM.map(lm => lm.toString + " -> " + mLMToTips.getOrElse(lm, "NONE")).mkString("mLMToTips:\n    ", "\n    ", ""),
			lItem.map(item => L3A_PipetteItem.toDebugString(Seq(item)) + " -> " + mItemToTip.get(item).map(_.toString).getOrElse("MISSING")).mkString("mItemToTip:\n    ", "\n    ", ""),
			//lItem.map(item => Command.getWellsDebugString(Seq(item.dest)) + " -> " + mDestToTip(item)).mkString("mDestToTip:\n    ", "\n    ", ""),
			"mTipToWashIntensityA:\n    "+mTipToCleanSpecA.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			"mTipToWashIntensityPendingA:\n    "+mTipToCleanSpecPendingA.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			"mTipToWashIntensity:\n    "+mTipToCleanSpec.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			"mTipToWashIntensityPending:\n    "+mTipToCleanSpecPending.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			lDispense.map(twvpString).mkString("lDispense:\n    ", "\n    ", ""),
			lAspirate.map(twvpString).mkString("lAspirate:\n    ", "\n    ", "")
		).mkString("GroupA(\n  ", "\n  ", ")\n")
	}
	
	private def twvpString(twvp: TipWellVolumePolicy): String = {
		List(twvp.tip, Command.getWellsDebugString(Seq(twvp.well)), twvp.nVolume, twvp.policy.id).mkString(", ")			
	}
}
