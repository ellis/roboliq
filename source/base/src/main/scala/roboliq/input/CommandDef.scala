package roboliq.input

import scala.beans.BeanProperty
import roboliq.entities.Rel

object ParamInput extends Enumeration {
	val Required, Optional, Plannable = Value
}

case class ParamDef(
	name: String,
	typ: String,
	input: ParamInput.Value
)

case class CommandCall(
	name: String,
	params: List[String]
)

case class LabeledCommandCallList(
	label: String,
	commands: List[CommandCall]
)

object CommandType extends Enumeration {
	val Task, Procedure, Function, Instruction = Value
}

case class CommandDef(
	name: String,
	typ: CommandType.Value,
	implements_? : Option[String],
	description_? : Option[String],
	documentation_? : Option[String],
	params: List[ParamDef],
	effects: List[Rel],
	commands: List[CommandCall],
	oneOf: List[LabeledCommandCallList]
)
