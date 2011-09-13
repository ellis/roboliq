package roboliq.robots.evoware.commands

import roboliq.common._


case class L2C_EvowareFacts(args: L12A_EvowareFactsArgs) extends CommandL2 {
	type L1Type = L1C_EvowareFacts
	
	def updateState(builder: StateBuilder) {
		// No state
	}
	
	def toL1(states: RobotState): Result[L1Type] = {
		Success(L1C_EvowareFacts(args))
	}
	
	override def toDebugString = {
		import args._
		this.getClass().getSimpleName() + List(sDevice, sVariable, sValue).mkString("(", ", ", ")") 
	}

}

case class L12A_EvowareFactsArgs(
	sDevice: String,
	sVariable: String,
	sValue: String
)

case class L1C_EvowareFacts(args: L12A_EvowareFactsArgs) extends CommandL1
