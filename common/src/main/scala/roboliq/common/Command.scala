package roboliq.common

trait Command
trait CommandL4 extends Command {
	type L3Type <: Command
	def addKnowledge(kb: KnowledgeBase)
	def toL3(states: RobotState): Either[Seq[String], L3Type]
}
trait CommandL2 extends Command {
	type L1Type <: Command
	def toL1(states: RobotState): Either[Seq[String], L1Type]
}

/*abstract class CommandL2 extends Command
abstract class CommandL3 extends Command
abstract class CommandL4 extends Command
*/