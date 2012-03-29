package roboliq.commands

import scala.collection.mutable.ArrayBuffer

import roboliq.common._


class PlateHandlingSetup {
	var replate: PlateObj = null
	var locationNew: Location = null
	var locationFinal: Location = null
	
	def toL3(states: RobotState): Result[PlateHandlingConfig] = {
		Success(new PlateHandlingConfig(
			replate_? = if (replate == null) None else Some(replate),
			locationNew_? = if (locationNew == null) None else Some(locationNew.state(states).location),
			locationFinal_? = if (locationFinal == null) None else Some(locationFinal.state(states).location)
		))
	}
}

class PlateHandlingConfig(
	val replate_? : Option[PlateObj],
	val locationNew_? : Option[String],
	val locationFinal_? : Option[String]
) {
		
	def getPreHandlingCommands(states: RobotState, plate: PlateConfigL2): Seq[Command] = {
		val cmds = new ArrayBuffer[Command]

		import move._
		val plateState = plate.state(states)
		locationNew_?.map(location => {
			cmds += L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(location), None))
		})
		
		cmds.toSeq
	}
		
	def getPostHandlingCommands(states: RobotState, plate: PlateConfigL2): Seq[Command] = {
		val cmds = new ArrayBuffer[Command]

		import move._
		val plateState = plate.state(states)
		locationFinal_?.map(location => {
			cmds += L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(location), None))
		})
		
		cmds.toSeq
	}
}
