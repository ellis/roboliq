package roboliq.commands.centrifuge

import roboliq.common._
import roboliq.commands._


case class L4C_Centrifuge(args: L4A_CentrifugeArgs) extends CommandL4 {
	type L3Type = L3C_Centrifuge
	
	val setup = new L4A_CentrifugeSetup

	def addKnowledge(kb: KnowledgeBase) {
		// TODO: note that plate will occupy the target location
		// TODO: request plate compatibility with this device
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		for { setupPlate <- setup.plateHandling.toL3(states) }
		yield L3C_Centrifuge(new L3A_CentrifugeArgs(
			idDevice_? = setup.idDevice_?,
			idProgram_? = setup.idProgram_?,
			plates = args.plates.map(_.state(states).conf),
			plateHandling = setupPlate
		))
	}
}

case class L3C_Centrifuge(args: L3A_CentrifugeArgs) extends CommandL3

class L4A_CentrifugeSetup {
	var idDevice_? : Option[String] = None
	var idProgram_? : Option[String] = None
	val plateHandling = new PlateHandlingSetup
}

class L4A_CentrifugeArgs(
	val plates: Seq[PlateObj]
)

class L3A_CentrifugeArgs(
	val idDevice_? : Option[String],
	val idProgram_? : Option[String],
	val plates: Seq[PlateConfigL2],
	val plateHandling: PlateHandlingConfig
)
