package roboliq.commands.pipette.scheduler

import scala.collection.immutable.SortedSet

import roboliq.core._
import roboliq.commands.pipette._


/**
 * @param mLM map from ALL items (not just the items in this group) to the chosen LM for the destination well
 * @param mTipToLM map from tip to source LM
 * @param mItemToTip map from item to tip used for that item 
 * @param mTipToCleanSpecA clean specs for individual tips
 * @param mTipToCleanSpec clean specs for tips which will be cleaned (more tips may be cleaned than is strictly necessary, because entire blocks of tips may need to be cleaned at the same time)
 */
class GroupA(
	val mItemToState: Map[Item, ItemState],
	val mLM: Map[Item, LM],
	val states0: RobotState,
	val tipBindings0: Map[Tip, LM],
	val mTipToCleanSpecPending0: Map[Tip, WashSpec],
	val lItem: Seq[Item],
	val lLM: Seq[LM],
	val mLMToItems: Map[LM, Seq[Item]],
	val mLMData: Map[LM, LMData],
	val mLMTipCounts: Map[LM, Int],
	val mLMToTips: Map[LM, SortedSet[Tip]],
	val mTipToLM: Map[Tip, LM],
	val mItemToTip: Map[Item, Tip],
	val mTipToVolume: Map[Tip, LiquidVolume],
	val mItemToPolicy: Map[Item, PipettePolicy],
	val mTipToPolicy: Map[Tip, PipettePolicy],
	val mTipToCleanSpecA: Map[Tip, WashSpec],
	val mTipToCleanSpecPendingA: Map[Tip, WashSpec],
	val mTipToCleanSpec: Map[Tip, WashSpec],
	val mTipToCleanSpecPending: Map[Tip, WashSpec],
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
		tipBindings0: Map[Tip, LM] = tipBindings0,
		mTipToCleanSpecPending0: Map[Tip, WashSpec] = mTipToCleanSpecPending0,
		lItem: Seq[Item] = lItem,
		lLM: Seq[LM] = lLM,
		mLMToItems: Map[LM, Seq[Item]] = mLMToItems,
		mLMData: Map[LM, LMData] = mLMData,
		mLMTipCounts: Map[LM, Int] = mLMTipCounts,
		mLMToTips: Map[LM, SortedSet[Tip]] = mLMToTips,
		mTipToLM: Map[Tip, LM] = mTipToLM,
		mItemToTip: Map[Item, Tip] = mItemToTip,
		mTipToVolume: Map[Tip, LiquidVolume] = mTipToVolume,
		mItemToPolicy: Map[Item, PipettePolicy] = mItemToPolicy,
		mTipToPolicy: Map[Tip, PipettePolicy] = mTipToPolicy,
		mTipToCleanSpecA: Map[Tip, WashSpec] = mTipToCleanSpecA,
		mTipToCleanSpecPendingA: Map[Tip, WashSpec] = mTipToCleanSpecPendingA,
		mTipToCleanSpec: Map[Tip, WashSpec] = mTipToCleanSpec,
		mTipToCleanSpecPending: Map[Tip, WashSpec] = mTipToCleanSpecPending,
		lDispense: Seq[TipWellVolumePolicy] = lDispense,
		lAspirate: Seq[TipWellVolumePolicy] = lAspirate,
		lPremix: Seq[TipWellMix] = lPremix,
		lPostmix: Seq[TipWellMix] = lPostmix,
		bClean: Boolean = bClean,
		states1: RobotState = states1
	): GroupA = {
		new GroupA(mItemToState, mLM, states0, tipBindings0, mTipToCleanSpecPending0, lItem, lLM, mLMToItems, mLMData, mLMTipCounts, mLMToTips, mTipToLM, mItemToTip, mTipToVolume,
				mItemToPolicy, mTipToPolicy, mTipToCleanSpecA, mTipToCleanSpecPendingA, mTipToCleanSpec, mTipToCleanSpecPending, lDispense, lAspirate, lPremix, lPostmix, bClean,
				states1)
	}
	
	override def toString: String = {
		List(
			"lItem:\n    "+Item.toDebugString(lItem),
			"mLMToItems:\n"+mLMToItems.toSeq.map(pair => pair._1.toString + " -> " + Item.toDebugString(pair._2)).mkString("    ", "\n    ", ""),
			lLM.map(lm => lm.toString + " -> " + mLMData(lm)).mkString("mLMData:\n    ", "\n    ", ""),
			lLM.map(lm => lm.toString + " -> " + mLMTipCounts.getOrElse(lm, 0)).mkString("mLMTipCounts:\n    ", "\n    ", ""),
			lLM.map(lm => lm.toString + " -> " + mLMToTips.getOrElse(lm, "NONE")).mkString("mLMToTips:\n    ", "\n    ", ""),
			lItem.map(item => Item.toDebugString(Seq(item)) + " -> " + mItemToTip.get(item).map(_.toString).getOrElse("MISSING")).mkString("mItemToTip:\n    ", "\n    ", ""),
			//lItem.map(item => Command.getWellsDebugString(Seq(item.dest)) + " -> " + mDestToTip(item)).mkString("mDestToTip:\n    ", "\n    ", ""),
			"mTipToCleanIntensityA:\n    "+mTipToCleanSpecA.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			"mTipToCleanIntensityPendingA:\n    "+mTipToCleanSpecPendingA.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			"mTipToCleanIntensity:\n    "+mTipToCleanSpec.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			"mTipToCleanIntensityPending:\n    "+mTipToCleanSpecPending.mapValues(_.washIntensity).toSeq.sortBy(_._1).mkString(", "),
			lDispense.map(twvpString).mkString("lDispense:\n    ", "\n    ", ""),
			lAspirate.map(twvpString).mkString("lAspirate:\n    ", "\n    ", ""),
			lPremix.map(twmString).mkString("lPremix:\n    ", "\n    ", ""),
			lPostmix.map(twmString).mkString("lPostmix:\n    ", "\n    ", "")
		).filter(!_.toString.isEmpty).mkString("GroupA(\n  ", "\n  ", ")\n")
	}
	
	private def twvpString(twvp: TipWellVolumePolicy): String = {
		List(twvp.tip, Printer.getWellsDebugString(Seq(twvp.well)), twvp.volume, twvp.policy.id).mkString(", ")			
	}
	
	private def twmString(twvp: TipWellMix): String = {
		List(twvp.tip, Printer.getWellsDebugString(Seq(twvp.well)), twvp.mixSpec.volume, twvp.mixSpec.count, twvp.mixSpec.mixPolicy).mkString(", ")			
	}
}
