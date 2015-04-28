package roboliq.input

import roboliq.ai.strips
import roboliq.core.ResultC


@RjsJsonType("protocolData")
case class ProtocolData(
	val variables: RjsBasicMap = RjsBasicMap(),
	val objects: RjsBasicMap = RjsBasicMap(),
	val commands: Map[String, RjsValue] = Map(),
	val planningDomainObjects: Map[String, String] = Map(),
	val planningInitialState: strips.Literals = strips.Literals.empty,
	val processingState_? : Option[ProcessingState] = None
) {
	def merge(that: ProtocolData): ResultC[ProtocolData] = {
		for {
			variables <- this.variables merge that.variables
			objects <- this.objects merge that.objects
		} yield {
			new ProtocolData(
				variables = variables,
				objects = objects,
				commands = this.commands ++ that.commands,
				planningDomainObjects = this.planningDomainObjects ++ that.planningDomainObjects,
				planningInitialState = this.planningInitialState ++ that.planningInitialState,
				processingState_? = that.processingState_? orElse this.processingState_?
			)
		}
	}
}

case class ProcessingState(
	variables: Map[String, ProcessingVariable],
	tasks: Map[String, ProcessingTask],
	commands: Map[String, ProcessingCommand],
	plan: ProcessingPlan
)

/**
 * @param setter Identifier for where the value comes from, e.g. "user"
 */
case class ProcessingVariable(
	name: String,
	value_? : Option[RjsBasicValue],
	setter_? : Option[String],
	validations: List[CommandValidation] = Nil,
	alternatives: List[RjsBasicValue]
)

/**
 * @param value_? Name of method to use for this task
 * @param setter_? Identifier for where the value comes from, e.g. "user", "automatic"
 */
case class ProcessingTask(
	commandId: String,
	value_? : Option[String],
	setter_? : Option[String],
	validations: List[CommandValidation] = Nil,
	alternatives: List[String]
)

/**
 * (not used for tasks -- for tasks, see ProcessingTask)
 * @param function One of "ExpandMethod", "HandleAction"
 */
case class ProcessingCommand(
	commandId: String,
	function: String,
	input: RjsBasicMap,
	result: RjsBasicValue,
	validations: List[CommandValidation] = Nil,
	effects: strips.Literals = strips.Literals.empty
)

case class ProcessingPlan(
	pddl_? : Option[String] = None,
	partialPlan_? : Option[RjsBasicMap] = None,
	partialOrder_ll : List[List[String]] = Nil,
	completeOrder_l_? : Option[List[String]] = None
)

case class CommandValidation(
	message: String,
	param_? : Option[String] = None,
	precond_? : Option[Int] = None
)

/*
case class CommandInfo(
	command: RjsValue,
	successors: List[String] = Nil,
	validations: List[CommandValidation] = Nil,
	effects: strips.Literals = strips.Literals.empty
)*/

/*
sealed trait CommandValidation
case class CommandValidation_Param(name: String) extends CommandValidation
case class CommandValidation_Precond(description: String) extends CommandValidation

case class MyPlate(
	model_? : Option[String],
	location_? : Option[String]
)

case class ProtocolDataA(
	val objects: RjsBasicMap = RjsBasicMap(),
	val commands: RjsBasicMap = RjsBasicMap(),
	val commandOrderingConstraints: List[List[String]] = Nil,
	val commandOrder: List[String] = Nil,
	val planningDomainObjects: Map[String, String] = Map(),
	val planningInitialState: strips.Literals = strips.Literals.empty
) {
	def merge(that: ProtocolDataA): ResultC[ProtocolDataA] = {
		for {
			objects <- this.objects merge that.objects
			commands <- this.commands merge that.commands
		} yield {
			new ProtocolDataA(
				objects = objects,
				commands = commands,
				commandOrderingConstraints = this.commandOrderingConstraints ++ that.commandOrderingConstraints,
				commandOrder = this.commandOrder ++ that.commandOrder,
				planningDomainObjects = this.planningDomainObjects ++ that.planningDomainObjects,
				planningInitialState = this.planningInitialState ++ that.planningInitialState
			)
		}
	}
}
*/
