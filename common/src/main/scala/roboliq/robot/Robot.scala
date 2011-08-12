package roboliq.robot

import scala.collection.immutable.SortedSet

import roboliq.parts._
import roboliq.tokens._

//case class PipetteClass(sName: String, policy: PipettePolicy)

trait Robot {
	val partTop = new Part

	val config: RobotConfig
	//def state: RobotState
	/** Minimum volume which can be aspirated */
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double
	/** Maximum volume of the given liquid which this tip can hold */
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double
	/** Choose aspirate method */
	def getAspiratePolicy(tipState: TipState, wellState: WellState): Option[PipettePolicy]
	/** Choose dispense method */
	def getDispensePolicy(tipState: TipState, wellState: WellState, nVolume: Double): Option[PipettePolicy]
	//def getAspiratePolicy(sAspirateClass: String): Option[PipettePolicy]
	//def getDispensePolicy(sDispenseClass: String): Option[PipettePolicy]
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]]
	def batchesForAspirate(twvps: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]]
	def batchesForDispense(twvps: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]]
	def batchesForClean(tcs: Seq[Tuple2[Tip, CleanDegree.Value]]): Seq[T1_Clean]
	def score(tokens: Seq[T1_Token]): Int
}
