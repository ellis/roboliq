package roboliq.input

import roboliq.ai.strips
import roboliq.core.ResultC
import scala.annotation.tailrec


case class ProtocolData(
	val variables: Map[String, ProtocolDataVariable] = Map(),
	val materials: Map[String, Material] = Map(),
	val steps: Map[String, ProtocolDataStep] = Map(),
	val labObjects: Map[String, LabObject] = Map(),
	val planningDomainObjects: Map[String, String] = Map(),
	val planningInitialState: strips.Literals = strips.Literals.empty,
	val processingState_? : Option[ProcessingState] = None
) {
	def merge(that: ProtocolData): ResultC[ProtocolData] = {
		for {
			variables <- RjsConverterC.mergeObjects(this.variables, that.variables)
			materials <- RjsConverterC.mergeObjects(this.materials, that.materials)
			steps <- RjsConverterC.mergeObjects(this.steps, that.steps)
			labObjects <- RjsConverterC.mergeObjects(this.labObjects, that.labObjects)
		} yield {
			new ProtocolData(
				variables = variables,
				materials = materials,
				steps = steps,
				labObjects = labObjects,
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

/**
 * @param setter Identifier for where the value comes from, e.g. "user"
 */
case class ProtocolVariableInfo(
	setter_? : Option[String],
	messages: List[String],
	alternatives: List[RjsBasicValue]
)


class ProtocolDataStep(
	val params: Map[String, RjsBasicValue],
	val children: Map[String, ProtocolDataStep]
) {
	def getChild(ident: String): ResultC[ProtocolDataStep] = {
		@tailrec
		def step(l: List[String], cm: ProtocolDataStep): ResultC[ProtocolDataStep] = {
			l match {
				case Nil => ResultC.unit(cm)
				case part :: l1 =>
					cm.children.get(part) match {
						case None => ResultC.error(s"Step not found: `$ident`")
						case Some(cm1) => step(l1, cm1)
					}
			}
		}
		val l = ident.split(".").toList
		step(l, this)
	}
}

object ProtocolDataStep {
	def from(m: RjsBasicMap): ResultC[ProtocolDataStep] = {
		// The child steps are fields that start with digits
		val (params, children0) = m.map.partition(x => !x._1(0).isDigit)
		for {
			child_l <- ResultC.mapAll(children0.toList) { case (name, v) =>
				v match {
					case m1: RjsBasicMap => from(m1).map(name -> _)
					case _ => ResultC.error(s"Parameter `$name` must have a Map value, because it starts with a digit and represents a sub-step.  Instead it has this value: $v")
				}
			}
		} yield {
			new ProtocolDataStep(params, child_l.toMap)
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
	`type`: String,
	value_? : Option[RjsBasicValue],
	setter_? : Option[String],
	validations: List[CommandValidation],
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
	validations: List[CommandValidation],
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

materials:
  flask: {type: Plate}
  microfuge_tube: {type: Plate}
  sterile_microfuge_tube: {type: Plate}

  medium:
    type: Liquid
    label: "Rich medium (LB, YT, or Terrific medium) containing appropriate antibiotic"
  sol1:
    type: Liquid
    label: "Alkaline Lysis Solution I"
    description: "50 mM Glucose, 25 mM Tris-HCl (pH 8.0), 10 mM EDTA (pH 8.0)"
  sol2:
    type: Liquid
    label: "freshly prepared Alkaline Lysis Solution II"
    description: "0.2 N NaOH, 1% SDS (w/v)"
  sol3:
    type: Liquid
    label: "Alkaline Lysis Solution II"
    description: "5 M sodium acetate, glacial acetic acid"
  bacteria:
    type: Cells
    label: "a single colony of transformed bacteria"

sources:
  medium:
    well: flask(A01)

steps:
  1:
    
*/
	
	def fromProtocol(protocol: Protocol): ResultC[ProtocolData] = {
		val variables = protocol.variables.map { case (name, x) =>
			name -> ProtocolDataVariable(name, x.description, x.`type`, x.value, x.alternatives)
		}
		for {
			nameToMaterial_l <- ResultC.mapAll(protocol.materials.toList) { case (name, m) =>
				for {
					typ <- RjsConverterC.fromRjs[String](m, "type")
					label_? <- RjsConverterC.fromRjs[Option[String]](m, "label")
					description_? <- RjsConverterC.fromRjs[Option[String]](m, "description")
					material <- typ match {
						case "Plate" => ResultC.unit(PlateMaterial(name, typ, label_?, description_?))
						case "Liquid" => ResultC.unit(LiquidMaterial(name, typ, label_?, description_?))
						case _ => ResultC.error(s"Unrecognized type: `$typ`")
					}
				} yield name -> material
			}
			step_l <- ResultC.mapAll(protocol.steps.toList) { case (name, m) =>
				ProtocolDataStep.from(m).map(name -> _)
			}
		} yield {
			val materials = nameToMaterial_l.toMap
			val planningDomainObjects = nameToMaterial_l.map({ case (name, x) => name -> x.`type`.toLowerCase })
			ProtocolData(
				variables = variables,
				materials = materials,
				steps = step_l.toMap,
				planningDomainObjects = planningDomainObjects.toMap,
				planningInitialState = strips.Literals.empty, //planningInitialState,
				processingState_? = None
			)
		}
	}

/*
	/**
	 * Extract ProtocolData from a high-level protocol description.
	 */
	def extractProtocolData(protocol: RjsProtocol): ResultC[ProtocolData] = {
		val command_l = protocol.commands.zipWithIndex.map { case (rjsval, i) =>
			(i+1).toString -> rjsval
		}
		val n = command_l.size
		val commandOrderingConstraint_l =
			(1 to n).toList.map(i => i.toString :: (if (i < n) List((i+1).toString) else Nil))
		val commandOrder_l =
			(1 to n).toList.map(_.toString)
		def convMapToBasic[A <: RjsValue](map: Map[String, A]): ResultC[Map[String, RjsBasicValue]] = {
			for {
				l <- ResultC.map(map.toList) { case (name, rjsval) =>
					RjsValue.fromValueToBasicValue(rjsval).map(name -> _)
				}
			} yield l.toMap
		}
		for {
			labware_m <- convMapToBasic(protocol.labwares)
			substance_m <- convMapToBasic(protocol.substances)
			source_m <- convMapToBasic(protocol.sources)
			command2_l <- ResultC.map(command_l.toList) { case (name, rjsval) =>
				RjsValue.toBasicValue(rjsval).map(name -> _)
			}
		} yield {
			val objects = RjsBasicMap(labware_m ++ substance_m ++ source_m)
			println("objects: "+objects)
			val (planningDomainObjects, planningInitialState) = processLabware(protocol.labwares)
			println("planningDomainObjects: "+planningDomainObjects)
			ProtocolData(
				objects = objects,
				commands = command2_l.toMap,
				planningDomainObjects = planningDomainObjects,
				planningInitialState = planningInitialState
			)
		}
	}
	
	private def processLabware(
		labware_m: Map[String, RjsProtocolLabware]
	): (Map[String, String], strips.Literals) = {
		val objectToType_m = new HashMap[String, String]
		val atom_l = new ArrayBuffer[strips.Atom]
		for ((name, plate) <- labware_m) {
			objectToType_m += (name -> "plate")
			atom_l += strips.Atom("labware", Seq(name))
			plate.model_?.foreach(model => atom_l += strips.Atom("model", Seq(name, model)))
			plate.location_?.foreach(location => atom_l += strips.Atom("location", Seq(name, location)))
		}
		(objectToType_m.toMap, strips.Literals(atom_l.toList, Nil))
	}
*/
}