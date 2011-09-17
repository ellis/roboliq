package roboliq.robots.evoware.devices.pcr.trobot

import roboliq.common._
import roboliq.commands._


object L12F_PcrClose extends L12F[PcrDevice_TRobot] {
	type ProgramParams = Unit
	def updateState(builder: StateBuilder, cmd2: L2C) {
		cmd2.args.device.stateWriter(builder).open(false)
	}
}

object L12F_PcrOpen extends L12F[PcrDevice_TRobot] {
	type ProgramParams = Unit
	def updateState(builder: StateBuilder, cmd2: L2C) {
		cmd2.args.device.stateWriter(builder).open(true)
	}
}

object L12F_PcrRun extends L12F[PcrDevice_TRobot] {
	type ProgramParams = Tuple2[Int, Int]
	def updateState(builder: StateBuilder, cmd2: L2C) {
		cmd2.args.device.stateWriter(builder).open(false)
	}
}