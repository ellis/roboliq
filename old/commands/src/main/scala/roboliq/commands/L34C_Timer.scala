package roboliq.commands

import roboliq.common._


case class L4C_Timer(args: L4A_TimerArgs) extends CommandL4 {
	type L3Type = L3C_Timer

	def addKnowledge(kb: KnowledgeBase) {
		// Nothing to do
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		args.toL3(states) match {
			case Error(lsErrors) => Error(lsErrors)
			case Success(args3) => Success(new L3C_Timer(args3))
		}
	}

}

case class L3C_Timer(args: L3A_TimerArgs) extends CommandL3

class L4A_TimerArgs {
	val setup = new L4A_TimerSetup
	
	def toL3(states: RobotState): Result[L3A_TimerArgs] = {
		val nSeconds = setup.nSeconds_? match {
			case None => return Error(Seq("duration of timer must be set"))
			case Some(n) => n
		}
		Success(new L3A_TimerArgs(
			nSeconds
		))
	}
}

class L4A_TimerSetup {
	var nSeconds_? : Option[Int] = None
}

class L3A_TimerArgs(
	val nSeconds: Int
)
