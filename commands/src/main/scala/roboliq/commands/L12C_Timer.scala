package roboliq.commands

import roboliq.common._


case class L2C_Timer(args: L12A_TimerArgs) extends CommandL2 {
	type L1Type = L1C_Timer
	
	def updateState(builder: StateBuilder) {
		// Nothing to do
	}
	
	def toL1(states: RobotState): Either[Seq[String], L1Type] = {
		Right(L1C_Timer(args))
	}
	
	override def toDebugString = {
		import args._
		this.getClass().getSimpleName() + List(nSeconds).mkString("(", ", ", ")") 
	}
}

case class L12A_TimerArgs(
	nSeconds: Int
)

case class L1C_Timer(args: L12A_TimerArgs) extends CommandL1 {
	override def toDebugString = {
		import args._
		this.getClass().getSimpleName() + List(nSeconds).mkString("(", ", ", ")") 
	}
}
