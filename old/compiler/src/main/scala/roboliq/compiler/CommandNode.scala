package roboliq.compiler

import roboliq.common._


class CommandNode(val cmd: Command) {
	var warnings: List[String] = Nil
	var errors: List[String] = Nil
	var translation: List[Command] = Nil
	var bComplete = false
}
