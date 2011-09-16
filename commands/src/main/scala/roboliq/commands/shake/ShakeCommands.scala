package roboliq.commands.shake

import roboliq.common._
import roboliq.commands._


trait ShakeCommands extends RoboliqCommands {
	def shake(plate: Plate, nDuration: Int): L4A_ShakeSetup = {
		val args = new L4A_ShakeArgs(plate, nDuration, None)
		val cmd = L4C_Shake(args)
		cmds += cmd
		cmd.setup
	}
}
