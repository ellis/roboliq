package roboliq.robots.evoware.devices.roboseal

import roboliq.common._
import roboliq.commands._


object L12F_RoboSeal extends L12F[RoboSealDevice] {
	type ProgramParams = Unit
	def updateState(builder: StateBuilder, cmd2: L2C) {
		// do nothing
	}
}
