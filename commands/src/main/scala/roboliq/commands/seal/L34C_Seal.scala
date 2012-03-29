package roboliq.commands.seal

import roboliq.common._
import roboliq.commands._


case class L4C_Seal(args: L4A_SealArgs) extends CommandL4 {
	type L3Type = L3C_Seal
	
	val setup = new L4A_SealSetup

	def addKnowledge(kb: KnowledgeBase) {
		// TODO: note that plate will occupy the target location
		// TODO: request plate compatibility with this device
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		for { setupPlate <- setup.plateHandling.toL3(states) }
		yield L3C_Seal(new L3A_SealArgs(
			idDevice_? = setup.idDevice_?,
			idProgram_? = setup.idProgram_?,
			plate = args.plate.state(states).conf,
			plateHandling = setupPlate
		))
	}
}

case class L3C_Seal(args: L3A_SealArgs) extends CommandL3

class L4A_SealSetup {
	var idDevice_? : Option[String] = None
	var idProgram_? : Option[String] = None
	val plateHandling = new PlateHandlingSetup
}

class L4A_SealArgs(
	val plate: PlateObj
)

class L3A_SealArgs(
	val idDevice_? : Option[String],
	val idProgram_? : Option[String],
	val plate: PlateConfigL2,
	val plateHandling: PlateHandlingConfig
)
