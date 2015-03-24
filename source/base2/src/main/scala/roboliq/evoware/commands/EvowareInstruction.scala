package roboliq.evoware.commands

import roboliq.evoware.translator.L0C_Command
import roboliq.input.Instruction

case class EvowareInstruction(l0c: L0C_Command) extends Instruction {
	val effects = Nil
	val data = Nil
}

case class EvowareSubroutine(path: String) extends Instruction {
	val effects = Nil
	val data = Nil
}
