package roboliq.commands.move

import roboliq.common._


case class L4C_MovePlate(args: L4A_MovePlateArgs) extends CommandL4 {
	type L3Type = L3C_MovePlate

	def addKnowledge(kb: KnowledgeBase) {
		// TODO: note that plate will occupy the target location
		// TODO: handle lidHandlingSpec too, e.g. location and noting that plate model needs a lid, and that plate should have a lid on initially
	}
	
	def toL3(states: RobotState): Either[Seq[String], L3Type] = {
		args.toL3(states) match {
			case Left(lsErrors) => Left(lsErrors)
			case Right(args3) => Right(new L3C_MovePlate(args3))
		}
	}

}

case class L3C_MovePlate(args: L3A_MovePlateArgs) extends CommandL3

class L4A_MovePlateArgs(
	val plate: Plate,
	val location: String,
	val lidHandlingSpec_? : Option[LidHandlingSpec]
) {
	def toL3(states: RobotState): Either[Seq[String], L3A_MovePlateArgs] = {
		Right(new L3A_MovePlateArgs(
			plate.state(states).conf,
			location,
			lidHandlingSpec_?
		))
	}
}

class L3A_MovePlateArgs(
	val plate: PlateConfigL2,
	val location: String,
	val lidHandlingSpec_? : Option[LidHandlingSpec]
)
