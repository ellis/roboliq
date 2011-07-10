package roboliq.level2.commands

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._
import roboliq.level2.tokens._


class T2_PipettePlate_Compiler(robot: Robot, state0: RobotState, tok: T2_PipettePlate) {
	val tokens: Seq[T1_TokenState] = {
		val wws: Seq[Tuple2[Well, Well]] = tok.src.wells zip tok.dest.wells
		val list = wws.map(ww -> (ww._1, ww._2, tok.nVolume)) 
		val tok2 = T2_Pipette(list)
		val comp = new T2_Pipette_Compiler(robot, state0, tok2)
		comp.tokens
	}
}
