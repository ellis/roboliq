package roboliq.commands

import roboliq.common._


case class L4C_Timer(args: L4A_TimerArgs) extends CommandL4 {
	type L3Type = L3C_Timer

	def addKnowledge(kb: KnowledgeBase) {
		// Nothing to do
	}
	
	def toL3(states: RobotState): Either[Seq[String], L3Type] = {
		args.toL3(states) match {
			case Left(lsErrors) => Left(lsErrors)
			case Right(args3) => Right(new L3C_Timer(args3))
		}
	}

}

case class L3C_Timer(args: L3A_TimerArgs) extends CommandL3

class L4A_TimerArgs {
	val setup = new L4A_TimerSetup
	
	def toL3(states: RobotState): Either[Seq[String], L3A_TimerArgs] = {
		val nSeconds = setup.nSeconds_? match {
			case None => return Left(Seq("duration of timer must be set"))
			case Some(n) => n
		}
		Right(new L3A_TimerArgs(
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
