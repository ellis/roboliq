package roboliq.commands.system

import roboliq.common._
import roboliq.commands._


case class L4C_Execute(args: LA_Execute) extends CommandL4 {
	type L3Type = L3C_Execute

	def addKnowledge(kb: KnowledgeBase) {
		// Nothing to do
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		Success(new L3C_Execute(args))
	}
}

// REFACTOR: make better parameters out of these once I have the opportunity to find out what they mean! -- ellis, 2011-11-18
class LA_Execute(val cmd: String, var bWaitTillDone: Boolean, var bCheckResult: Boolean)

case class L3C_Execute(args: LA_Execute) extends CommandL3

case class L2C_Execute(args: LA_Execute) extends CommandL2 {
	type L1Type = L1C_Execute
	
	def updateState(builder: StateBuilder) {
		// No state
	}
	
	def toL1(states: RobotState): Result[L1Type] = {
		Success(L1C_Execute(args))
	}
	
	override def toDebugString = {
		this.getClass().getSimpleName() + List(args.cmd, args.bWaitTillDone, args.bCheckResult).mkString("(", ", ", ")") 
	}
}

case class L1C_Execute(args: LA_Execute) extends CommandL1
