package roboliq.pipette.planners

import scala.collection.immutable.SortedSet

import roboliq.core._
import roboliq.entities._


abstract class PipetteDevice {
	/** Can the device use the given number of tip per model simultaneously? */
//	def supportTipModelCounts(tipModelCounts: Map[TipModel, Int]): Result[Boolean]
//	def assignTips(tipsFree: SortedSet[Tip], tipModel: TipModel, nTips: Int): Result[SortedSet[Tip]]
//	def areTipsDisposable: Boolean
	def getDispenseAllowableTipModels(tipModel_l: List[TipModel], liquid: Liquid, volume: LiquidVolume): List[TipModel] = {
		tipModel_l.filter(tipModel => {
			volume >= tipModel.volumeMin && volume <= tipModel.volume
		})
	}
//	/** Minimum volume which can be aspirated */
//	def getTipAspirateVolumeMin(tip: TipState, liquid: Liquid): LiquidVolume
//	/** Maximum volume of the given liquid which this tip can hold */
//	def getTipHoldVolumeMax(tip: TipState, liquid: Liquid): LiquidVolume
//	/** Choose aspirate method */
//	def getAspiratePolicy(tipState: TipState, nVolume: LiquidVolume, wellState: VesselState): Option[PipettePolicy]
//	/** Choose dispense method */
//	def getDispensePolicy(liquid: Liquid, tipModel: TipModel, nVolume: LiquidVolume, wellState: VesselState): Option[PipettePolicy]
//	def getMixSpec(tipState: TipState, wellState: VesselState, mixSpec_? : Option[MixSpecOpt]): Result[MixSpec]
//	def canBatchSpirateItems(lTwvp: List[TipWellVolumePolicy]): Boolean
//	def canBatchMixItems(lTwvp: List[TipWellMix]): Boolean
//	def getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll: SortedSet[Tip], lTipCleaning: SortedSet[Tip]): SortedSet[Tip]
//	def batchCleanTips(lTipAll: SortedSet[Tip]): Seq[SortedSet[Tip]]
//	def batchCleanSpecs(lTipAll: SortedSet[Tip], mTipToCleanSpec: Map[Tip, WashSpec]): Seq[Tuple2[WashSpec, SortedSet[Tip]]]

	def groupSpirateItems(l: List[TipWellVolumePolicy], state: WorldState): List[List[TipWellVolumePolicy]] = {
		case class A(twvp: TipWellVolumePolicy, tipModel_? : Option[TipModel])
		
		def step(l: List[A], group_r: List[A], acc_r: List[List[A]]): List[List[TipWellVolumePolicy]] = {
			l match {
				case Nil =>
					(group_r.reverse :: acc_r).reverse.map(_.map(_.twvp))
				case a :: rest =>
					if (group_r.isEmpty) {
						step(l.tail, List(a), acc_r)
					}
					else {
						val prev = group_r.head
						val group_# = a :: group_r
						// Can use up to three items to test equidistant-ness
						val check_l = group_#.take(3).map(_.twvp).reverse
						// Same tip model, pipette policy, and equidistant?
						val b1 = (a.tipModel_? == prev.tipModel_?)
						val b2 = (a.twvp.policy == prev.twvp.policy)
						val b3 = TipWell.equidistant(check_l, state)
						if (b1 && b2 && b3)
							step(l.tail, group_#, acc_r)
						else {
							//println(s"failed test: $b1, $b2, $b3: l=${l}, check_l=${check_l}")
							//check_l.foreach(twvp => println(twvp.tip.row, twvp.tip.col, twvp.well.index, twvp.well.row, twvp.well.col))
							step(l.tail, List(a), group_r.reverse :: acc_r)
						}
					}
			}
		}

		val l1 = l.map(twvp => A(twvp, state.getTipModel(twvp.tip)))
		step(l1, Nil, Nil)
	} 
}
