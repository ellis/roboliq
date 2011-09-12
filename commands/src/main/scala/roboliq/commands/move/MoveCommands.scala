package roboliq.commands.move

import roboliq.common._


trait MoveCommands extends RoboliqCommands {
	def movePlate(plate: Plate, location: Memento[String]) {
		val args = new L4A_MovePlateArgs(plate, location, None)
		val cmd = L4C_MovePlate(args)
		cmds += cmd
	}
}
