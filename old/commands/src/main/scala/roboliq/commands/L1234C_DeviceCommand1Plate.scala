package roboliq.commands

import roboliq.common._


case class L4C_DeviceCommand1Plate(idCommand: String, plate: PlateObj) extends CommandL4 {
	type L3Type = L3C_DeviceCommand1Plate
	
	val setup = new L4A_DeviceCommand1PlateSetup

	def addKnowledge(kb: KnowledgeBase) {
		// TODO: note that plate will occupy the target location
		// TODO: request plate compatibility with this device
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		for { ph <- setup.plateHandling.toL3(states) }
		yield L3C_DeviceCommand1Plate(new L3A_DeviceCommand1PlateArgs(
			idCommand,
			setup.idDevice_?,
			setup.idProgram_?,
			plate.state(states).conf,
			ph
		))
	}

	override def toDebugString = {
		import setup._
		this.getClass().getSimpleName() + List(idCommand, idDevice_?, idProgram_?).mkString("(", ", ", ")") 
	}
}

class L4A_DeviceCommand1PlateSetup {
	var idDevice_? : Option[String] = None
	var idProgram_? : Option[String] = None
	val plateHandling = new PlateHandlingSetup
}

case class L3C_DeviceCommand1Plate(args: L3A_DeviceCommand1PlateArgs) extends CommandL3 {
	override def toDebugString = {
		import args._
		this.getClass().getSimpleName() + List(idCommand, idDevice_?, idProgram_?).mkString("(", ", ", ")") 
	}
}

class L3A_DeviceCommand1PlateArgs(
	val idCommand: String,
	val idDevice_? : Option[String],
	val idProgram_? : Option[String],
	val plate: Plate,
	val plateHandling: PlateHandlingConfig
)

/** Derived class needs to define updateState() method */
abstract class L2C_DeviceCommand1PlateAbstract(args: L2A_DeviceCommand1PlateArgs) extends CommandL2 {
	type L1Type = L1C_DeviceCommand1Plate
	
	def toL1(states: RobotState): Result[L1Type] = {
		val location = args.plate.state(states).location
		import args._
		Success(L1C_DeviceCommand1Plate(new L1A_DeviceCommand1PlateArgs(
			idCommand, idDevice, idProgram, plate, location
		)))
	}
	
	override def toDebugString = {
		import args._
		this.getClass().getSimpleName() + List(idCommand, idDevice, idProgram).mkString("(", ", ", ")") 
	}
}

case class L2A_DeviceCommand1PlateArgs(
	val idCommand: String,
	val idDevice: String,
	val idProgram: String,
	val plate: Plate
)

case class L1C_DeviceCommand1Plate(args: L1A_DeviceCommand1PlateArgs) extends CommandL1 {
	override def toDebugString = {
		import args._
		this.getClass().getSimpleName() + List(idCommand, idDevice, idProgram).mkString("(", ", ", ")") 
	}
}

case class L1A_DeviceCommand1PlateArgs(
	val idCommand: String,
	val idDevice: String,
	val idProgram: String,
	val plate: Plate,
	val location: String
)
