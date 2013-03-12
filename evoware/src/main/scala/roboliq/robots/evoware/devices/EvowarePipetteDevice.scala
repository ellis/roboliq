package roboliq.robots.evoware.devices

import scala.collection.immutable.SortedSet

import roboliq.core._,roboliq.entity._
import roboliq.commands.pipette._
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
			val nExtra = CleanIntensity.max(tip.cleanDegreePending, liquid.tipCleanPolicy.exit) match {
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
	def canBatchSpirateItems(lTwvp: List[TipWellVolumePolicy]): Boolean = {
		// Ensure that all items use the same policy and same tip models
		val b1 = lTwvp match {
			case Nil => true
			case x :: xs =>
				val tipState = x.tip
				xs.forall(twvp => twvp.policy == x.policy && twvp.tip.model_? == tipState.model_?)
		}
		// Ensure that all intra-tip distances are equal to the intra-well distances
		val b2 = roboliq.robots.evoware.Utils.equidistant(lTwvp)
		// Ensure that all wells are in the same column
		val b3 = WellGroup(lTwvp.map(_.well)).splitByCol().size == 1
		
		// FIXME: for debug only
		//if (lTwvp.size == 2 && lTwvp.head.well.index == 0) {
			//println("!!!!!!!!!!!")
			//println((lTwvp, b1, b2, b3))
		//}
		
		b1 && b2 && b3
	}
	
	//private case class Info(tipModel: TipModel, pipettePolicy: PipettePolicy, plate: Plate, row: Int, col: Int)
	def groupSpirateItems(l: List[TipWellVolumePolicy]): List[List[TipWellVolumePolicy]] = {
		type A = TipWellVolumePolicy
		def step(l: List[A], group_r: List[A], acc_r: List[List[A]]): List[List[A]] = {
			l match {
				case Nil =>
					(group_r.reverse :: acc_r).reverse
				case item :: rest =>
					if (group_r.isEmpty) {
						step(l.tail, List(item), acc_r)
					}
					else {
						val prev = group_r.head
						val group_# = item :: group_r
						// Can use up to three items to test equidistance-ness 
						val check_l = group_#.take(3).reverse
						// Same tip model, pipette policy, and equidistant?
						val b1 = (item.tip.model_? == prev.tip.model_?)
						val b2 = (item.policy == prev.policy)
						val b3 = roboliq.robots.evoware.Utils.equidistant(check_l)
						if (b1 && b2 && b3)
							step(l.tail, group_#, acc_r)
						else
							step(l.tail, List(item), group_r.reverse :: acc_r)
					}
			}
		}
		step(l, Nil, Nil)
	} 
	
	def canBatchMixItems(lTwvp: List[TipWellMix]): Boolean = {
		// Ensure that all items use the same policy, tip sizes, and mix repetitions
		val b1 = lTwvp match {
			case Nil => true
			case x :: xs =>
				val tipState = x.tip
				xs.forall(twvp => twvp.mixSpec.mixPolicy == x.mixSpec.mixPolicy && twvp.tip.model_? == tipState.model_? && twvp.mixSpec.count == x.mixSpec.count)
		}
		// Ensure that all intra-tip distances are equal to the intra-well distances
		val b2 = roboliq.robots.evoware.Utils.equidistant(lTwvp)
		// Ensure that all wells are in the same column
		val b3 = WellGroup(lTwvp.map(_.well)).splitByCol().size == 1
		
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
