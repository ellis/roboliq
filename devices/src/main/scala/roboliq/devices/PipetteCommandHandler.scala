/*package roboliq.devices

import roboliq.parts._
import roboliq.robot._
import roboliq.compiler._
import roboliq.level3._

import roboliq.devices.pipette._


class PipetteCommandHandler(compiler: Compiler, device: PipetteDevice) extends CommandHandlerL1 with CommandHandlerL2 with CommandHandlerL3 {
	def updateState(state0: RobotState, cmd: Command): Option[RobotState] = {
		val builder = new RobotStateBuilder(state0)
		var bFound = true
		cmd match {
			case L1_Aspirate(twvps) => twvps.foreach(builder.aspirate)
			case L1_Dispense(_) => Some(1)
			case L1_Clean(_, _) => Some(1)
			case _ => bFound = false 
		}
		if (bFound) Some(builder.toImmutable) else None
	}
	
	def score(state0: RobotState, res: CompileFinal): Option[Int] = {
		res.cmd match {
			case L1_Aspirate(_) => Some(1)
			case L1_Dispense(_) => Some(1)
			case L1_Clean(_, _) => Some(1)
			case _ => None
		}
	}

	def compile2(state0: RobotState, cmd: Command): Option[CompileResult] = {
		cmd match {
			case c @ L2_PipetteCommand(args) =>
				val h = new Compiler_PipetteCommandL2(compiler, device, state0, c)
				Some(CompileTranslation(cmd, h.translation))
			case _ => None
		}
	}

	def addKnowledge(kb: KnowledgeBase, cmd: Command): Boolean = {
		cmd match {
			case c @ PipetteCommand(_, _) =>
				val h = new Compiler_PipetteCommand(kb, c)
				h.addKnowledge()
				true
			case _ =>
				false
		}
	}
	
	def compile3(kb: KnowledgeBase, cmd: Command): Option[CompileResult] = {
		cmd match {
			case c @ PipetteCommand(_, _) =>
				val h = new Compiler_PipetteCommand(kb, c)
				val errs = h.checkParams()
				val r = if (errs.isEmpty) {
					val translation = h.compile()
					CompileTranslation(cmd, translation)
				}
				else {
					CompileError(cmd, errs.map(_.message))
				}
				Some(r)
			case _ =>
				None
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
*/