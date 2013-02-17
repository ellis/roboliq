package roboliq.robots.evoware.devices

import scala.collection.immutable.SortedSet

import roboliq.core._
import roboliq.commands.pipette._
//import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.robots.evoware._


abstract class EvowarePipetteDevice extends PipetteDevice {
	def areTipsDisposable: Boolean
	
	def getTipAspirateVolumeMin(tip: TipState, liquid: Liquid): LiquidVolume =
		tip.model_? match {
			case None => LiquidVolume.empty
			case Some(model) => model.volumeMin
		}
	
	def getTipHoldVolumeMax(tip: TipState, liquid: Liquid): LiquidVolume = {
		tip.model_?.map(tipModel => {
			val nExtra = CleanIntensity.max(tip.cleanDegreePending, liquid.group.cleanPolicy.exit) match {
				case CleanIntensity.Decontaminate => tipModel.volumeDeconExtra
				case _ => tipModel.volumeWashExtra
			}
			tipModel.volume - nExtra
		}).getOrElse(LiquidVolume.empty)
	}
	
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]] = {
		val tips2 = tips.toIndexedSeq
		val wells2 = wells.toSeq.zipWithIndex
		for ((well, i) <- wells2) yield {
			val i2 = i % tips2.size
			tips2(i2) -> well
		}
	}
		
	/**
	 * Ensure that all items use the same policy and same tip sizes
	 */
	def canBatchSpirateItems(states: StateMap, lTwvp: List[TipWellVolumePolicy]): Boolean = {
		// Ensure that all items use the same policy and same tip sizes
		val b1 = lTwvp match {
			case Nil => true
			case x :: xs =>
				val tipState = x.tip.state(states)
				xs.forall(twvp => twvp.policy == x.policy && twvp.tip.state(states).model_? == tipState.model_?)
		}
		// Ensure that all intra-tip distances are equal to the intra-well distances
		val b2 = roboliq.robots.evoware.Utils.equidistant(lTwvp)
		// Ensure that all wells are in the same column
		val b3 = WellGroup(states, lTwvp.map(_.well)).splitByCol().size == 1
		
		// FIXME: for debug only
		//if (lTwvp.size == 2 && lTwvp.head.well.index == 0) {
			//println("!!!!!!!!!!!")
			//println((lTwvp, b1, b2, b3))
		//}
		
		b1 && b2 && b3
	}
	
	def canBatchMixItems(states: StateMap, lTwvp: List[TipWellMix]): Boolean = {
		// Ensure that all items use the same policy, tip sizes, and mix repetitions
		val b1 = lTwvp match {
			case Nil => true
			case x :: xs =>
				val tipState = x.tip.state(states)
				xs.forall(twvp => twvp.mixSpec.mixPolicy_? == x.mixSpec.mixPolicy_? && twvp.tip.state(states).model_? == tipState.model_? && twvp.mixSpec.nCount_? == x.mixSpec.nCount_?)
		}
		// Ensure that all intra-tip distances are equal to the intra-well distances
		val b2 = roboliq.robots.evoware.Utils.equidistant(lTwvp)
		// Ensure that all wells are in the same column
		val b3 = WellGroup(states, lTwvp.map(_.well)).splitByCol().size == 1
		
		b1 && b2 && b3
	}


	/*
	private case class KeySpirate(plate: Plate, iTipType: Int) {
		def this(item: L2A_SpirateItem) = this(item.well.holder, item.tip.index / 4)
	}
	def batchesForAspirate(items: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]] = {
		items.groupBy(item => new KeySpirate(item)).values.toSeq
	}
	def batchesForDispense(items: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]] = {
		items.groupBy(item => new KeySpirate(item)).values.toSeq
	}
	def batchesForClean(tcs: Seq[Tuple2[TipConfigL2, CleanIntensity.Value]]): Seq[Seq[Tuple2[TipConfigL2, CleanIntensity.Value]]] = Seq(tcs)
	def batchesForMix(items: Seq[L2A_MixItem]): Seq[Seq[L2A_MixItem]] = {
		items.groupBy(item => KeySpirate(item.well.holder, item.tip.index / 4)).values.toSeq
	}
	*/
}
