package roboliq.commands.shake

import roboliq.common._
import roboliq.commands._


case class L4C_Shake(args: L4A_ShakeArgs) extends CommandL4 {
	type L3Type = L3C_Shake
	
	val setup = new L4A_ShakeSetup

	def addKnowledge(kb: KnowledgeBase) {
		// TODO: note that plate will occupy the target location
		// TODO: request plate compatibility with this device
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		for { setupPlate <- setup.plate.toL3(states) }
		yield L3C_Shake(new L3A_ShakeArgs(
			plate = args.plate.state(states).conf,
			nDuration = args.nDuration,
			idDevice_? = args.idDevice_?,
			setup = setupPlate
		))
	}
}

case class L3C_Shake(args: L3A_ShakeArgs) extends CommandL3

class L4A_ShakeSetup {
	var idDevice_? : Option[String] = None
	val idProgram_? : Option[String] = None
	val plateHandling = new PlateHandlingSetup
}

class L4A_ShakeArgs(
	val plate: Plate,
	val nDuration: Int,
	val idDevice_? : Option[String]
)

class L3A_ShakeArgs(
	val plate: PlateConfigL2,
	val nDuration: Int,
	val idDevice_? : Option[String],
	val setup: L3A_PlateSetup
)
