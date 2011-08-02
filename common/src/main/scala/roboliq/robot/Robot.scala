package roboliq.robot

import scala.collection.immutable.SortedSet

import roboliq.parts._
import roboliq.tokens._


trait Robot {
	val partTop = new Part

	val config: RobotConfig
	//def state: RobotState
	/** Minimum volume which can be aspirated */
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double
	/** Maximum volume of the given liquid which this tip can hold */
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double
	/** Choose dispense method */
	//def getPipettePolicy(tip: Tip, liquid: Liquid, nVolume: Double, wellState: WellState): PipettePolicy
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]]
	def batchesForAspirate(state: IRobotState, twvs: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]]
	def batchesForDispense(state: IRobotState, twvs: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]]
	def batchesForClean(tcs: Seq[Tuple2[Tip, CleanDegree.Value]]): Seq[T1_Clean]
	def score(tokens: Seq[T1_Token]): Int
}
