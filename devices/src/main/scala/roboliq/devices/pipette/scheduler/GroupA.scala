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
class GroupA(
	val mItemToState: Map[Item, ItemState],
	val mLM: Map[Item, LM],
	val states0: RobotState,
	val tipBindings0: Map[TipConfigL2, LM],
	val mTipToCleanSpecPending0: Map[TipConfigL2, WashSpec],
	val lItem: Seq[Item],
	val lLM: Seq[LM],
	val mLMToItems: Map[LM, Seq[Item]],
	val mLMData: Map[LM, LMData],
	val mLMTipCounts: Map[LM, Int],
	val mLMToTips: Map[LM, SortedSet[TipConfigL2]],
	val mTipToLM: Map[TipConfigL2, LM],
	val mItemToTip: Map[Item, TipConfigL2],
	val mTipToVolume: Map[TipConfigL2, Double],
	val mItemToPolicy: Map[Item, PipettePolicy],
	val mTipToCleanSpecA: Map[TipConfigL2, WashSpec],
	val mTipToCleanSpecPendingA: Map[TipConfigL2, WashSpec],
	val mTipToCleanSpec: Map[TipConfigL2, WashSpec],
	val mTipToCleanSpecPending: Map[TipConfigL2, WashSpec],
	val lDispense: Seq[TipWellVolumePolicy],
	val lAspirate: Seq[TipWellVolumePolicy],
	val lPremix: Seq[TipWellMix],
	val lPostmix: Seq[TipWellMix],
	val bClean: Boolean,
	val states1: RobotState
) {
	def copy(
		mItemToState: Map[Item, ItemState] = mItemToState,
		mLM: Map[Item, LM] = mLM,
		states0: RobotState = states0,
		tipBindings0: Map[TipConfigL2, LM] = tipBindings0,
		mTipToCleanSpecPending0: Map[TipConfigL2, WashSpec] = mTipToCleanSpecPending0,
		lItem: Seq[Item] = lItem,
		lLM: Seq[LM] = lLM,
		mLMToItems: Map[LM, Seq[Item]] = mLMToItems,
		mLMData: Map[LM, LMData] = mLMData,
		mLMTipCounts: Map[LM, Int] = mLMTipCounts,
		mLMToTips: Map[LM, SortedSet[TipConfigL2]] = mLMToTips,
		mTipToLM: Map[TipConfigL2, LM] = mTipToLM,
		mItemToTip: Map[Item, TipConfigL2] = mItemToTip,
		mTipToVolume: Map[TipConfigL2, Double] = mTipToVolume,
		mItemToPolicy: Map[Item, PipettePolicy] = mItemToPolicy,
		mTipToCleanSpecA: Map[TipConfigL2, WashSpec] = mTipToCleanSpecA,
		mTipToCleanSpecPendingA: Map[TipConfigL2, WashSpec] = mTipToCleanSpecPendingA,
		mTipToCleanSpec: Map[TipConfigL2, WashSpec] = mTipToCleanSpec,
		mTipToCleanSpecPending: Map[TipConfigL2, WashSpec] = mTipToCleanSpecPending,
		lDispense: Seq[TipWellVolumePolicy] = lDispense,
		lAspirate: Seq[TipWellVolumePolicy] = lAspirate,
		lPremix: Seq[TipWellMix] = lPremix,
		lPostmix: Seq[TipWellMix] = lPostmix,
		bClean: Boolean = bClean,
		states1: RobotState = states1
	): GroupA = {
		new GroupA(mItemToState, mLM, states0, tipBindings0, mTipToCleanSpecPending0, lItem, lLM, mLMToItems, mLMData, mLMTipCounts, mLMToTips, mTipToLM, mItemToTip, mTipToVolume,
				mItemToPolicy, mTipToCleanSpecA, mTipToCleanSpecPendingA, mTipToCleanSpec, mTipToCleanSpecPending, lDispense, lAspirate, lPremix, lPostmix, bClean,
				states1)
	}
	
	override def toString: String = {
		List(
			"lItem:\n    "+L3A_PipetteItem.toDebugString(lItem),
			"mLMToItems:\n"+mLMToItems.toSeq.map(pair => pair._1.toString + " -> " + L3A_PipetteItem.toDebugString(pair._2)).mkString("    ", "\n    ", ""),
			lLM.map(lm => lm.toString + " -> " + mLMData(lm)).mkString("mLMData:\n    ", "\n    ", ""),
			lLM.map(lm => lm.toString + " -> " + mLMTipCounts.getOrElse(lm, 0)).mkString("mLMTipCounts:\n    ", "\n    ", ""),
			lLM.map(lm => lm.toString + " -> " + mLMToTips.getOrElse(lm, "NONE")).mkString("mLMToTips:\n    ", "\n    ", ""),
			lItem.map(item => L3A_PipetteItem.toDebugString(Seq(item)) + " -> " + mItemToTip.get(item).map(_.toString).getOrElse("MISSING")).mkString("mItemToTip:\n    ", "\n    ", ""),
			//lItem.map(item => Command.getWellsDebugString(Seq(item.dest)) + " -> " + mDestToTip(item)).mkString("mDestToTip:\n    ", "\n    ", ""),
			"mTipToWashIntensityA:\n    "+mTipToCleanSpecA.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			"mTipToWashIntensityPendingA:\n    "+mTipToCleanSpecPendingA.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			"mTipToWashIntensity:\n    "+mTipToCleanSpec.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			"mTipToWashIntensityPending:\n    "+mTipToCleanSpecPending.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			lDispense.map(twvpString).mkString("lDispense:\n    ", "\n    ", ""),
			lAspirate.map(twvpString).mkString("lAspirate:\n    ", "\n    ", ""),
			lPremix.map(twmString).mkString("lPremix:\n    ", "\n    ", ""),
			lPostmix.map(twmString).mkString("lPostmix:\n    ", "\n    ", "")
		).filter(!_.toString.isEmpty).mkString("GroupA(\n  ", "\n  ", ")\n")
	}
	
	private def twvpString(twvp: TipWellVolumePolicy): String = {
		List(twvp.tip, Command.getWellsDebugString(Seq(twvp.well)), twvp.nVolume, twvp.policy.id).mkString(", ")			
	}
	
	private def twmString(twvp: TipWellMix): String = {
		List(twvp.tip, Command.getWellsDebugString(Seq(twvp.well)), twvp.mixSpec.nVolume, twvp.mixSpec.nCount, twvp.mixSpec.mixPolicy.id).mkString(", ")			
	}
}
