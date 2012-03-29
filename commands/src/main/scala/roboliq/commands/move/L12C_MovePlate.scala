package roboliq.commands.move

import roboliq.common._


case class L2C_MovePlate(args: L2A_MovePlateArgs) extends CommandL2 {
	type L1Type = L1C_MovePlate
	
	def updateState(builder: StateBuilder) {
		args.plate.obj.stateWriter(builder).location = args.locationDest
	}
	
	def toL1(states: RobotState): Result[L1Type] = {
		val args1 = args.toL1(states) match {
			case Error(lsError) => return Error(lsError)
			case Success(args1) => args1
		}
		Success(L1C_MovePlate(args1))
	}
	
	override def toDebugString = {
		import args._
		this.getClass().getSimpleName() + List(iRoma, plate, locationDest, lidHandling).mkString("(", ", ", ")") 
	}
}

case class L2A_MovePlateArgs(
	iRoma: Int, // 0 for RoMa1, 1 for RoMa2
	plate: Plate,
	locationDest: String,
	lidHandling: LidHandling.Value,
	locationLid: String
) {
	def toL1(states: StateMap): Result[L1A_MovePlateArgs] = {
		val plateState = plate.state(states)
		val sPlateModel = plate.model_? match {
			case None => return Error(Seq("plate \""+plate.sLabel+"\" must be assigned a plate model"))
			case Some(model) => model.id
		}
		Success(L1A_MovePlateArgs(
			iRoma = iRoma,
			sPlateModel = sPlateModel,
			locationSrc = plateState.location,
			locationDest = locationDest,
			lidHandling = lidHandling,
			locationLid = locationLid
		))
	}
}

case class L1C_MovePlate(args: L1A_MovePlateArgs) extends CommandL1 {
	override def toDebugString = {
		import args._
		this.getClass().getSimpleName() + List(iRoma, sPlateModel, locationDest, lidHandling).mkString("(", ", ", ")") 
	}
}

case class L1A_MovePlateArgs(
	iRoma: Int, // 0 for RoMa1, 1 for RoMa2
	sPlateModel: String,
	locationSrc: String,
	locationDest: String,
	lidHandling: LidHandling.Value,
	locationLid: String
)
