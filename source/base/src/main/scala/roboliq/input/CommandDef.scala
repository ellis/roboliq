package roboliq.input

import scala.beans.BeanProperty
import roboliq.entities.Rel
import roboliq.ai.plan.Strips
import roboliq.core.RsResult
import roboliq.ai.plan.Unique
import spray.json.JsValue

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
	precond: List[Strips.Literal],
	effect: List[Strips.Literal],
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
	precond: List[Strips.Literal],
	effect: List[Strips.Literal],
	output: List[CommandCall],
	oneOf: List[LabeledCommandCallList]
) {
	def createOperator(): RsResult[Strips.Operator] = {
		//println("A:")
		//println((precond ++ effect).flatMap(literal => literal.atom.params.filter(_.startsWith("$"))))
		val logicParam_l = (precond ++ effect).flatMap(literal => literal.atom.params.filter(_.startsWith("$"))).map(_.substring(1)).distinct
		val plannableParam_l = input.filter(_.mode == InputMode.Plannable).map(_.name)
		val unnecessaryPlannableParam_l = plannableParam_l.toSet -- plannableParam_l
		val use_l = logicParam_l.toSet
		val param_l = input.filter(input => use_l.contains(input.name))

		for {
			_ <- RsResult.assert(unnecessaryPlannableParam_l.isEmpty, s"'Plannable' parameters must be used in a 'precond' or and 'effect': "+unnecessaryPlannableParam_l.mkString(", "))
			_ <- RsResult.assert(param_l.size == use_l.size, "The following variables were used in 'precond' or 'effect', but not defined in 'input': "+(use_l -- param_l.map(_.name)).mkString(", "))
		} yield {
			Strips.Operator(
				name = name,
				paramName_l = param_l.map(_.name),
				paramTyp_l = param_l.map(_.`type`),
				preconds = Strips.Literals(Unique(precond :_ *)),
				effects = Strips.Literals(Unique(effect :_ *))
			)
		}
	}
	/*
	def evaluate(scope: Map[String, JsValue]): RsResult[JsValue] = {
		output match {
			case JsArray(l) =>
			case JsString
		}
	}
	*/
}
