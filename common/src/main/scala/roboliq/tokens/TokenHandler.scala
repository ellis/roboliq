package roboliq.tokens

import roboliq.robot.RobotState

trait TokenHandler {
	def translate2(state0: RobotState, tok: T2_Token): Seq[T1_TokenState]
}
