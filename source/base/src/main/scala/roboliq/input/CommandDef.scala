package roboliq.input

import scala.beans.BeanProperty
import roboliq.entities.Rel

object ParamInput extends Enumeration {
	val Required, Optional, Plannable = Value
}

case class ParamDef(
	name: String,
	`type`: String,
	input: ParamInput.Value
)

case class CommandCall(
	name: String,
	param: Map[String, String]
)

case class LabeledCommandCallList(
	label: String,
	command: List[CommandCall]
)

object CommandType extends Enumeration {
	val Task, Procedure, Function, Action, Operator, Instruction = Value
}

case class CommandDef(
	name: String,
	`type`: CommandType.Value,
	implements_? : Option[String],
	description_? : Option[String],
	documentation_? : Option[String],
	param: List[ParamDef],
	precond: List[Rel],
	effect: List[Rel],
	command: List[CommandCall],
	oneOf: List[LabeledCommandCallList]
)
