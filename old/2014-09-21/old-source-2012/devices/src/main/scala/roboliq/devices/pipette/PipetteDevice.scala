package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


abstract class PipetteDevice extends Device {
	val config: PipetteDeviceConfig
	
	def addKnowledge(kb: KnowledgeBase)
	def supportTipModelCounts(tipModelCounts: Map[TipModel, Int]): Result[Boolean]
	//def assignTips(tipsFree: SortedSet[TipConfigL2], tipModelCounts: Seq[Tuple2[TipModel, Int]]): Result[Seq[SortedSet[TipConfigL2]]]
	def assignTips(tipsFree: SortedSet[TipConfigL2], tipModel: TipModel, nTips: Int): Result[SortedSet[TipConfigL2]]
	def areTipsDisposable: Boolean
	def getDispenseAllowableTipModels(liquid: Liquid, nVolume: Double, nVolumeDest: Double): Seq[TipModel]
	/** Minimum volume which can be aspirated */
	def getTipAspirateVolumeMin(tip: TipStateL2, liquid: Liquid): Double
	/** Maximum volume of the given liquid which this tip can hold */
	def getTipHoldVolumeMax(tip: TipStateL2, liquid: Liquid): Double
	//def getPipettePolicy(sLiquidClass: String): Option[PipettePolicy]
	/** Choose aspirate method */
	def getAspiratePolicy(tipState: TipStateL2, nVolume: Double, wellState: WellStateL2): Option[PipettePolicy]
	/** Choose dispense method */
	def getDispensePolicy(liquid: Liquid, tip: TipConfigL2, nVolume: Double, wellState: WellStateL2): Option[PipettePolicy]
	def getMixSpec(tipState: TipStateL2, wellState: WellStateL2, mixSpec_? : Option[MixSpec]): Result[MixSpecL2]
	def canBatchSpirateItems(states: StateMap, lTwvp: List[TipWellVolumePolicy]): Boolean
	def canBatchMixItems(states: StateMap, lTwvp: List[TipWellMix]): Boolean
	def getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll: SortedSet[TipConfigL2], lTipCleaning: SortedSet[TipConfigL2]): SortedSet[TipConfigL2]
	//def getTipsToCleanSimultaneously(lTipAll: SortedSet[TipConfigL2], lTipCleaning: SortedSet[TipConfigL2]): SortedSet[TipConfigL2]
	def batchCleanTips(lTipAll: SortedSet[TipConfigL2]): Seq[SortedSet[TipConfigL2]]
	def batchCleanSpecs(lTipAll: SortedSet[TipConfigL2], mTipToCleanSpec: Map[TipConfigL2, WashSpec]): Seq[Tuple2[WashSpec, SortedSet[TipConfigL2]]]
	//def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]]
	//def chooseWashPrograms(tip: TipConfigL2, intensity: WashIntensity.Value): Seq[Int]
	/*def batchesForAspirate(twvps: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]]
	def batchesForDispense(twvps: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]]
	def batchesForClean(tcs: Seq[Tuple2[TipConfigL2, WashIntensity.Value]]): Seq[Seq[Tuple2[TipConfigL2, WashIntensity.Value]]]
	def batchesForMix(twvpcs: Seq[L2A_MixItem]): Seq[Seq[L2A_MixItem]]*/
}
