package roboliq.robots.evoware.commands

import roboliq.common._


case class L2C_EvowareSubroutine(sFilename: String, fnUpdateState: StateBuilder => Unit) extends CommandL2 {
	type L1Type = L1C_EvowareSubroutine
	
	def updateState(builder: StateBuilder) {
		fnUpdateState(builder)
	}
	
	def toL1(states: RobotState): Result[L1Type] = {
		Success(L1C_EvowareSubroutine(sFilename))
	}
	
	override def toDebugString = {
		this.getClass().getSimpleName() + List(sFilename).mkString("(", ", ", ")") 
	}

}

case class L1C_EvowareSubroutine(sFilename: String) extends CommandL1
