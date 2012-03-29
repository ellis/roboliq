package roboliq.commands.seal

import roboliq.common._
import roboliq.commands._


case class L4C_Peel(args: L4A_PeelArgs) extends CommandL4 {
	type L3Type = L3C_Peel
	
	val setup = new L4A_PeelSetup

	def addKnowledge(kb: KnowledgeBase) {
		// TODO: note that plate will occupy the target location
		// TODO: request plate compatibility with this device
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		for { setupPlate <- setup.plateHandling.toL3(states) }
		yield L3C_Peel(new L3A_PeelArgs(
			idDevice_? = setup.idDevice_?,
			idProgram_? = setup.idProgram_?,
			plate = args.plate.state(states).conf,
			plateHandling = setupPlate
		))
	}
}

case class L3C_Peel(args: L3A_PeelArgs) extends CommandL3

class L4A_PeelSetup {
	var idDevice_? : Option[String] = None
	var idProgram_? : Option[String] = None
	val plateHandling = new PlateHandlingSetup
}

class L4A_PeelArgs(
	val plate: PlateObj
)

class L3A_PeelArgs(
	val idDevice_? : Option[String],
	val idProgram_? : Option[String],
	val plate: PlateConfigL2,
	val plateHandling: PlateHandlingConfig
)
