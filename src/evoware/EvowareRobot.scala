package evoware

import scala.collection.mutable.ArrayBuffer

import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._


class EvowareRobot(val config: RobotConfig) extends Robot {
	var state: RobotState = RobotState.empty
	
	def chooseWellsForTips(tips: Seq[Tip], wells: Seq[Well]): Seq[Well] = {
		val chosen = new ArrayBuffer[Well]
		chosen ++= wells.take(tips.size)
		chosen
	}
	
	def score(tokens: Seq[Token]): Int = {
		tokens.size
	}
}
