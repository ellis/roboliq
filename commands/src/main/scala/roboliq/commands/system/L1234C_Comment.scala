package roboliq.commands.system

import roboliq.common._
import roboliq.commands._


case class L4C_Comment(s: String) extends CommandL4 {
	type L3Type = L3C_Comment

	def addKnowledge(kb: KnowledgeBase) {
		// Nothing to do
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		Success(new L3C_Comment(s))
	}

}

case class L3C_Comment(s: String) extends CommandL3

case class L2C_Comment(s: String) extends CommandL2 {
	type L1Type = L1C_Comment
	
	def updateState(builder: StateBuilder) {
		// No state
	}
	
	def toL1(states: RobotState): Result[L1Type] = {
		Success(L1C_Comment(s))
	}
	
	override def toDebugString = {
		this.getClass().getSimpleName() + List(s).mkString("(", ", ", ")") 
	}

}

case class L1C_Comment(s: String) extends CommandL1
