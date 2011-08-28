package roboliq.common

trait Command
trait CommandL4 extends Command {
	type L3Type <: CommandL3
	def addKnowledge(kb: KnowledgeBase)
	def toL3(states: RobotState): Either[Seq[String], L3Type]
}
trait CommandL3 extends Command
trait CommandL2 extends Command {
	type L1Type <: CommandL1
	def updateState(builder: StateBuilder)
	def toL1(states: RobotState): Either[Seq[String], L1Type]
}
trait CommandL1 extends Command

/*abstract class CommandL2 extends Command
abstract class CommandL3 extends Command
abstract class CommandL4 extends Command
*/