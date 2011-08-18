package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._
//import roboliq.parts._
//import roboliq.tokens._


trait PipetteDevice {
	val config: PipetteDeviceConfig
	/** Minimum volume which can be aspirated */
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double
	/** Maximum volume of the given liquid which this tip can hold */
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double
	/** Choose aspirate method */
	def getAspiratePolicy(tipState: TipStateL1, wellState: WellStateL1): Option[PipettePolicy]
	/** Choose dispense method */
	def getDispensePolicy(tipState: TipStateL1, wellState: WellStateL1, nVolume: Double): Option[PipettePolicy]
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]]
	def batchesForAspirate(twvps: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]]
	def batchesForDispense(twvps: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]]
	def batchesForClean(tcs: Seq[Tuple2[Tip, CleanDegree.Value]]): Seq[L1_Clean]
}

class PipetteDeviceGeneric extends PipetteDevice {
	val config = new PipetteDeviceConfig(
		tips = SortedSet((0 to 7).map(i => new Tip(i)) : _*),
		tipGroups = Array(Array(0,1,2,3), Array(4,5,6,7))
	)
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double = 0
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double = 1000
	//def getAspiratePolicy(tipState: TipState, wellState: WellState): Option[PipettePolicy]
	//def getDispensePolicy(tipState: TipState, wellState: WellState, nVolume: Double): Option[PipettePolicy]
	//def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]]
	def batchesForAspirate(twvps: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]] = Seq(twvps)
	def batchesForDispense(twvps: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]] = Seq(twvps)
	def batchesForClean(tcs: Seq[Tuple2[Tip, CleanDegree.Value]]): Seq[L1_Clean] = Seq(new L1_Clean(tcs.map(_._1), tcs.head._2))
}
