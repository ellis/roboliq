package roboliq.level3

import roboliq.compiler._
import roboliq.robot._

trait CommandHandler

trait CommandHandlerL1 extends CommandHandler {
	def compile1(cmd: Command): Option[CompileResult]
}

trait CommandHandlerL2 extends CommandHandler {
	def compile2(state0: RobotState, cmd: Command): Option[CompileResult]
}

trait CommandHandlerL3 extends CommandHandler {
	def addKnowledge(kb: KnowledgeBase, cmd: Command): Boolean
	def compile3(kb: KnowledgeBase, cmd: Command): Option[CompileResult]
}
