package roboliq.tokens

import roboliq.robot._

trait TokenHandler {
	val asTokens: Seq[String]
	
	def translate2(robot: Robot, state0: RobotState, tok: T2_Token): Seq[T1_TokenState]
}
