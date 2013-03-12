package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.core._, roboliq.entity._, roboliq.processor._
import roboliq.commands.pipette._


abstract class PipetteDevice {
	/** Can the device use the given number of tip per model simultaneously? */
	def supportTipModelCounts(tipModelCounts: Map[TipModel, Int]): Result[Boolean]
	def assignTips(tipsFree: SortedSet[Tip], tipModel: TipModel, nTips: Int): Result[SortedSet[Tip]]
	def areTipsDisposable: Boolean
	def getDispenseAllowableTipModels(tipModel_l: List[TipModel], liquid: Liquid, volume: LiquidVolume): List[TipModel] = {
		tipModel_l.filter(tipModel => {
			volume >= tipModel.volumeMin && volume <= tipModel.volume
		})
	}
	/** Minimum volume which can be aspirated */
	def getTipAspirateVolumeMin(tip: TipState, liquid: Liquid): LiquidVolume
	/** Maximum volume of the given liquid which this tip can hold */
	def getTipHoldVolumeMax(tip: TipState, liquid: Liquid): LiquidVolume
	/** Choose aspirate method */
	def getAspiratePolicy(tipState: TipState, nVolume: LiquidVolume, wellState: VesselState): Option[PipettePolicy]
	/** Choose dispense method */
	def getDispensePolicy(liquid: Liquid, tipModel: TipModel, nVolume: LiquidVolume, wellState: VesselState): Option[PipettePolicy]
	def getMixSpec(tipState: TipState, wellState: VesselState, mixSpec_? : Option[MixSpecOpt]): Result[MixSpec]
	def canBatchSpirateItems(lTwvp: List[TipWellVolumePolicy]): Boolean
	def canBatchMixItems(lTwvp: List[TipWellMix]): Boolean
	def getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll: SortedSet[Tip], lTipCleaning: SortedSet[Tip]): SortedSet[Tip]
	def batchCleanTips(lTipAll: SortedSet[Tip]): Seq[SortedSet[Tip]]
	def batchCleanSpecs(lTipAll: SortedSet[Tip], mTipToCleanSpec: Map[Tip, WashSpec]): Seq[Tuple2[WashSpec, SortedSet[Tip]]]

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
						val b3 = TipWell.equidistant(check_l)
						if (b1 && b2 && b3)
							step(l.tail, group_#, acc_r)
						else
							step(l.tail, List(item), group_r.reverse :: acc_r)
					}
			}
		}
		step(l, Nil, Nil)
	} 
}
