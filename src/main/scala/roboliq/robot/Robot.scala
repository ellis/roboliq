package roboliq.robot

import roboliq.parts._
import roboliq.tokens._


object DispenseKind extends Enumeration {
	val Free, WetContact, DryContact = Value
}

trait Robot {
	val config: RobotConfig
	def state: RobotState
	/** Minimum volume which can be aspirated */
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double
	/** Maximum volume of the given liquid which this tip can hold */
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double
	/** Choose dispense method */
	def getDispenseKind(tip: Tip, liquid: Liquid, nVolume: Double, wellState: WellState): DispenseKind.Value
	def chooseWellsForTips(tips: Seq[Tip], wells: Seq[Well]): Seq[Well]
	def score(tokens: Seq[Token]): Int
}
