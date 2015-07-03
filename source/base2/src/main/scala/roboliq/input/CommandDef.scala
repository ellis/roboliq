package roboliq.input

import scala.beans.BeanProperty
import roboliq.ai.strips
import roboliq.ai.plan.Unique
import spray.json.JsValue
import roboliq.core.ResultC

object InputMode extends Enumeration {
	val Required, Optional, Plannable = Value
}

case class InputDef(
	name: String,
	`type`: String,
	mode: InputMode.Value
)

case class CommandCall(
	call: String,
	input: Map[String, JsValue]
)

case class LabeledCommandCallList(
	label: String,
	command: List[CommandCall]
)

object CommandType extends Enumeration {
	val Task, Procedure, Function, Action, Operator, Instruction = Value
}

case class ActionDef(
	name: String,
	description_? : Option[String],
	documentation_? : Option[String],
	param: List[InputDef],
	precond: List[strips.Literal],
	effect: List[strips.Literal],
	output: JsValue
) {
}

case class CommandDef(
	name: String,
	`type`: CommandType.Value,
	implements_? : Option[String],
	description_? : Option[String],
	documentation_? : Option[String],
	input: List[InputDef],
	precond: List[strips.Literal],
	effect: List[strips.Literal],
	output: List[CommandCall],
	oneOf: List[LabeledCommandCallList]
) {
	def createOperator(): ResultC[strips.Operator] = {
		//println("A:")
		//println((precond ++ effect).flatMap(literal => literal.atom.params.filter(_.startsWith("$"))))
		val logicParam_l = (precond ++ effect).flatMap(literal => literal.atom.params.filter(_.startsWith("$"))).map(_.substring(1)).distinct
		val plannableParam_l = input.filter(_.mode == InputMode.Plannable).map(_.name)
		val unnecessaryPlannableParam_l = plannableParam_l.toSet -- plannableParam_l
		val use_l = logicParam_l.toSet
		val param_l = input.filter(input => use_l.contains(input.name))

		for {
			_ <- ResultC.assert(unnecessaryPlannableParam_l.isEmpty, s"'Plannable' parameters must be used in a 'precond' or and 'effect': "+unnecessaryPlannableParam_l.mkString(", "))
			_ <- ResultC.assert(param_l.size == use_l.size, "The following variables were used in 'precond' or 'effect', but not defined in 'input': "+(use_l -- param_l.map(_.name)).mkString(", "))
		} yield {
			strips.Operator(
				name = name,
				paramName_l = param_l.map(_.name),
				paramTyp_l = param_l.map(_.`type`),
				preconds = strips.Literals(Unique(precond :_ *)),
				effects = strips.Literals(Unique(effect :_ *))
			)
		}
	}
}
