/*package roboliq.commands.pcr

import roboliq.common._
import roboliq.commands._


case class L4C_Thermocycle(args: L4A_ThermocycleArgs) extends CommandL4 {
	type L3Type = L3C_Thermocycle
	
	val setup = new L4A_ThermocycleSetup

	def addKnowledge(kb: KnowledgeBase) {
		// TODO: note that plate will occupy the target location
		// TODO: request plate compatibility with this device
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		for { setupPlate <- setup.plateHandling.toL3(states) }
		yield L3C_Thermocycle(new L3A_ThermocycleArgs(
			idDevice_? = setup.idDevice_?,
			idProgram_? = setup.idProgram_?,
			plate = args.plate.state(states).conf,
			nDuration = args.nDuration,
			plateHandling = setupPlate
		))
	}
}

case class L3C_Thermocycle(args: L3A_ThermocycleArgs) extends CommandL3

class L4A_ThermocycleSetup {
	var idDevice_? : Option[String] = None
	var idProgram_? : Option[String] = None
	val plateHandling = new PlateHandlingSetup
}

class L4A_ThermocycleArgs(
	val plate: Plate,
	val nDuration: Int
)

class L3A_ThermocycleArgs(
	val idDevice_? : Option[String],
	val idProgram_? : Option[String],
	val plate: PlateConfigL2,
	val nDuration: Int,
	val plateHandling: PlateHandlingConfig
)
*/