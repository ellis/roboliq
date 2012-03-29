package roboliq.commands.move

import roboliq.common._


case class L4C_MovePlate(args: L4A_MovePlateArgs) extends CommandL4 {
	type L3Type = L3C_MovePlate

	def addKnowledge(kb: KnowledgeBase) {
		kb.addPlate(args.plate)
		kb.addObject(args.location)
		// TODO: note that plate will occupy the target location
		// TODO: handle lidHandlingSpec too, e.g. location and noting that plate model needs a lid, and that plate should have a lid on initially
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		args.toL3(states) match {
			case Error(lsErrors) => Error(lsErrors)
			case Success(args3) => Success(new L3C_MovePlate(args3))
		}
	}
}

case class L3C_MovePlate(args: L3A_MovePlateArgs) extends CommandL3

class L4A_MovePlateArgs(
	val plate: PlateObj,
	val location: Memento[String],
	val lidHandlingSpec_? : Option[LidHandlingSpec]
) {
	def toL3(states: RobotState): Result[L3A_MovePlateArgs] = {
		Success(new L3A_MovePlateArgs(
			plate.state(states).conf,
			ValueArg(location.state(states).value),
			lidHandlingSpec_?
		))
	}
}

class L3A_MovePlateArgs(
	val plate: PlateConfigL2,
	val location: ValueArg[String],
	val lidHandlingSpec_? : Option[LidHandlingSpec]
)
