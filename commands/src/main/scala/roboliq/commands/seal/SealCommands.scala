package roboliq.commands.seal

import roboliq.common._
import roboliq.commands._


trait SealCommands extends RoboliqCommands {
	def seal(plate: Plate): L4A_SealSetup = {
		val args = new L4A_SealArgs(plate)
		val cmd = L4C_Seal(args)
		cmds += cmd
		cmd.setup
	}

	def peel(plate: Plate): L4A_PeelSetup = {
		val args = new L4A_PeelArgs(plate)
		val cmd = L4C_Peel(args)
		cmds += cmd
		cmd.setup
	}
}
