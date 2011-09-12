package roboliq.commands

import roboliq.common._


class L4A_PlateSetup {
	var replate: Plate = null
	var locationNew: Location = null
	var locationFinal: Location = null
	
	def toL3(states: RobotState): Either[Seq[String], L3A_PlateSetup] = {
		Right(new L3A_PlateSetup(
			replate_? = if (replate == null) None else Some(replate),
			locationNew_? = if (locationNew == null) None else Some(locationNew.state(states).location),
			locationFinal_? = if (locationFinal == null) None else Some(locationFinal.state(states).location)
		))
	}
}

class L3A_PlateSetup(
	val replate_? : Option[Plate],
	val locationNew_? : Option[String],
	val locationFinal_? : Option[String]
)