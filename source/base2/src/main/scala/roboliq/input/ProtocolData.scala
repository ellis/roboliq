package roboliq.input

import roboliq.ai.strips
import roboliq.core.ResultC


case class ProtocolData(
	val variables: Map[String, ProtocolDataVariable] = Map(),
	val objects: Map[String, ProtocolDataObject] = Map(),
	val commands: Map[String, RjsValue] = Map(),
	val planningDomainObjects: Map[String, String] = Map(),
	val planningInitialState: strips.Literals = strips.Literals.empty,
	val processingState_? : Option[ProcessingState] = None
) {
	def merge(that: ProtocolData): ResultC[ProtocolData] = {
		for {
			variables <- RjsConverterC.mergeObjects(this.variables, that.variables)
			objects <- RjsConverterC.mergeObjects(this.objects, that.objects)
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

case class ProtocolDataVariable(
	name: String,
	description_? : Option[String] = None,
	type_? : Option[String] = None,
	value_? : Option[RjsBasicValue] = None,
	alternatives: List[RjsBasicValue] = Nil
)

case class ProtocolDataObject(
	CONTINUE
)

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
	`type`: String,
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

object ProtocolData {
/*
 * ProtocolData:
	val variables: Map[String, ProtocolDataVariable] = Map(),
	val objects: RjsBasicMap = RjsBasicMap(),
	val commands: Map[String, RjsValue] = Map(),
	val planningDomainObjects: Map[String, String] = Map(),
	val planningInitialState: strips.Literals = strips.Literals.empty,
	val processingState_? : Option[ProcessingState] = None
*/
/*
	Protocol:
	variables: Map[String, ProtocolVariable],
	labwares: Map[String, ProtocolLabware],
	substances: Map[String, ProtocolSubstance],
	sources: Map[String, ProtocolSource],
	commands: List[RjsValue]

description: Plasmid DNA Extraction - Miniprep (Excerpt)

labware:
  flask:
  microfuge_tube:
  sterile_microfuge_tube:

substances:
  medium:
    label: "Rich medium (LB, YT, or Terrific medium) containing appropriate antibiotic"
  sol1:
    label: "Alkaline Lysis Solution I"
    description: "50 mM Glucose, 25 mM Tris-HCl (pH 8.0), 10 mM EDTA (pH 8.0)"
  sol2:
    label: "freshly prepared Alkaline Lysis Solution II"
    description: "0.2 N NaOH, 1% SDS (w/v)"
  sol3:
    label: "Alkaline Lysis Solution II"
    description: "5 M sodium acetate, glacial acetic acid"
  bacteria:
    type: solid
    label: "a single colony of transformed bacteria"

sources:
  medium:
    well: flask(A01)

steps:
- 
    
*/
	
	def fromProtocol(protocol: Protocol): ResultC[ProtocolData] = {
		val variables = protocol.variables.map { case (name, x) =>
			name -> ProtocolDataVariable(name, x.description, x.`type`, x.value, x.alternatives)
		}
		val commands = protocol.commands.zipWithIndex.map({ case (x, i) =>
			(i+1).toString -> x
		}).toMap
		for {
			nameToLabware_l <- ResultC.mapAll(protocol.labwares.toList) { case (name, x) =>
				RjsValue.fromObject(x).map(name -> _.asInstanceOf[RjsBasicMap].add("name", RjsString(name)))
			}
		} yield {
			val objects = nameToLabware_l.toMap
			val planningDomainObjects = nameToLabware_l.map({ case (name, x) => name -> x.g})
			ProtocolData(
				variables,
				objects,
				commands,
				planningDomainObjects,
				planningInitialState,
				None
			)
		}
	}
}