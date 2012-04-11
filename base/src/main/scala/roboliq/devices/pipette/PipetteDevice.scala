package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.core._
import roboliq.commands.pipette._


abstract class PipetteDevice extends DeviceBean {
	def getTipModels: List[TipModel]
	def getTips: SortedSet[Tip]
	
	def supportTipModelCounts(tipModelCounts: Map[TipModel, Int]): Result[Boolean]
	def assignTips(tipsFree: SortedSet[Tip], tipModel: TipModel, nTips: Int): Result[SortedSet[Tip]]
	def areTipsDisposable: Boolean
	def getDispenseAllowableTipModels(liquid: Liquid, nVolume: LiquidVolume, nVolumeDest: LiquidVolume): Seq[TipModel]
	/** Minimum volume which can be aspirated */
	def getTipAspirateVolumeMin(tip: TipState, liquid: Liquid): LiquidVolume
	/** Maximum volume of the given liquid which this tip can hold */
	def getTipHoldVolumeMax(tip: TipState, liquid: Liquid): LiquidVolume
	/** Choose aspirate method */
	def getAspiratePolicy(tipState: TipState, nVolume: LiquidVolume, wellState: WellState): Option[PipettePolicy]
	/** Choose dispense method */
	def getDispensePolicy(liquid: Liquid, tipModel: TipModel, nVolume: LiquidVolume, wellState: WellState): Option[PipettePolicy]
	def getMixSpec(tipState: TipState, wellState: WellState, mixSpec_? : Option[MixSpec]): Result[MixSpec]
	def canBatchSpirateItems(states: StateMap, lTwvp: List[TipWellVolumePolicy]): Boolean
	def canBatchMixItems(states: StateMap, lTwvp: List[TipWellMix]): Boolean
	def getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll: SortedSet[Tip], lTipCleaning: SortedSet[Tip]): SortedSet[Tip]
	def batchCleanTips(lTipAll: SortedSet[Tip]): Seq[SortedSet[Tip]]
	def batchCleanSpecs(lTipAll: SortedSet[Tip], mTipToCleanSpec: Map[Tip, WashSpec]): Seq[Tuple2[WashSpec, SortedSet[Tip]]]
}
