package roboliq.devices

import roboliq.robot._
import roboliq.tokens._

import roboliq.level2._
import roboliq.level2.tokens._
import roboliq.devices.pipette._


class PipetteDevice extends TokenHandler {
	val asTokens = Array("pipette", "pipetteLiquid", "pipettePlate", "pipetteMix").toSeq
	
	def translate2(robot: Robot, state0: RobotState, tok: T2_Token): Seq[T1_TokenState] = {
		tok match {
			case t2 @ T2_Pipette(_) =>
				val comp = new T2_Pipette_Compiler(robot, state0, t2)
				comp.tokens
			case t2 @ T2_PipetteLiquid(_, _) =>
				val comp = new T2_PipetteLiquid_Compiler(robot, state0, t2)
				comp.tokens
			case t2 @ T2_PipettePlate(_, _, _) =>
				val comp = new T2_PipettePlate_Compiler(robot, state0, t2)
				comp.tokens
			case t2 @ T2_PipetteMix(_, _) =>
				val comp = new T2_PipetteMix_Compiler(robot, state0, t2)
				comp.tokens
		}
	}
}

object PipetteDeviceUtil {
	def updateState(state0: RobotState, tok: T1_Token): RobotState = {
		val builder = new RobotStateBuilder(state0)
		tok match {
			case T1_Aspirate(twvs) => twvs.foreach(builder.aspirate)
			case T1_Dispense(twvds) => twvds.foreach(builder.dispense)
			case t1 @ T1_Clean(_, _) => t1.tips.foreach(tip => builder.cleanTip(tip, t1.degree))
		}
		builder.toImmutable
	}
	
	def getTokenStates(state0: RobotState, toks: Seq[T1_Token]): Seq[T1_TokenState] = {
		var state = state0
		for (tok <- toks) yield {
			state = updateState(state, tok)
			new T1_TokenState(tok, state)
		}
	}
}
