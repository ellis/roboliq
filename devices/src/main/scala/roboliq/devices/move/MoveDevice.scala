package roboliq.devices.move

import roboliq.commands.move._


trait MoveDevice {
	def getRomaId(args: L3A_MovePlateArgs): Either[Seq[String], Int] = {
		Right(0)
	}
}