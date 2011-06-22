package roboliq.robot

import scala.collection.immutable.SortedSet

import roboliq.parts._
import roboliq.tokens._


trait Robot {
	val config: RobotConfig
	def state: RobotState
	/** Minimum volume which can be aspirated */
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double
	/** Maximum volume of the given liquid which this tip can hold */
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double
	/** Choose dispense method */
	def getDispenseKind(tip: Tip, liquid: Liquid, nVolume: Double, wellState: WellState): DispenseKind.Value
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]]
	def batchesForAspirate(twvs: Seq[TipWellVolume]): Seq[Seq[TipWellVolume]]
	def batchesForDispense(twvs: Seq[TipWellVolumeDispense]): Seq[Seq[TipWellVolumeDispense]]
	def score(tokens: Seq[T1_Token]): Int
}
