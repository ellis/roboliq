package roboliq.commands

import roboliq.common._


trait CommonCommands extends RoboliqCommands {
	def wait(nSeconds: Int): L4A_TimerSetup = {
		val args = new L4A_TimerArgs
		args.setup.nSeconds_? = Some(nSeconds)
		val cmd = L4C_Timer(args)
		cmds += cmd
		args.setup
	}
	
	def saveLocation(plate: PlateObj): Memento[String] = {
		val mem = new Memento[String]
		val cmd = L4C_SaveCurrentLocation(plate, mem)
		cmds += cmd
		mem
	}
}
