package roboliq.commands

import roboliq.common._


class PlateHandlingSetup {
	var replate: Plate = null
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
	val replate_? : Option[Plate],
	val locationNew_? : Option[String],
	val locationFinal_? : Option[String]
)