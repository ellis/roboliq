package roboliq.level3

import roboliq.compiler.Command


class CommandNode(val cmd: Command) {
	var warnings: List[String] = Nil
	var errors: List[String] = Nil
	var translation: List[Command] = Nil
	var bComplete = false
}
