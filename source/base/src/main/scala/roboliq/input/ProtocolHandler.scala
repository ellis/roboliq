package roboliq.input

import spray.json.JsObject
import spray.json.JsValue
import scala.collection.mutable.ArrayBuffer
import roboliq.ai.strips
import scala.collection.mutable.HashMap
import scala.math.Ordering.Implicits._
import scala.annotation.tailrec
import roboliq.ai.plan.Unique
import roboliq.core.ResultC

sealed trait CommandValidation
case class CommandValidation_Param(name: String) extends CommandValidation
case class CommandValidation_Precond(description: String) extends CommandValidation

case class MyPlate(
	model_? : Option[String],
	location_? : Option[String]
)

case class ProtocolDataA(
	val objects: RjsMap = RjsMap(),
	val commands: RjsMap = RjsMap(),
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

class ProtocolDataABuilder {
	private val objects = new HashMap[String, RjsValue]
	private val planningDomainObjects = new HashMap[String, String]
	private val planningInitialState = new ArrayBuffer[strips.Literal]
	
	def get: ProtocolDataA = {
		new ProtocolDataA(
			objects = RjsMap(objects.toMap),
			planningDomainObjects = planningDomainObjects.toMap,
			planningInitialState = strips.Literals(Unique(planningInitialState.toList : _*))
		)
	}
	
	def addObject(name: String, value: RjsValue) {
		objects(name) = value
	}
	
	def addPlanningDomainObject(name: String, typ: String) {
		planningDomainObjects(name) = typ
	}
	
	def addPlateModel(plateModelName: String, rjsPlateModel: RjsBasicMap) {
		addObject(plateModelName, rjsPlateModel)
		addPlanningDomainObject(plateModelName, rjsPlateModel.typ_?.get)
	}
	
	def addSiteModel(siteModelName: String) {
		planningDomainObjects(siteModelName) = "SiteModel"
	}
	
	def addSite(siteName: String) {
		planningDomainObjects(siteName) = "Site"
	}
	
	def addSite(name: String, value: RjsMap) {
		val typ = "Site"
		val value2 = value.add("type", RjsString(typ))
		addObject(name, value2)
		planningDomainObjects(name) = typ
	}
	
	/**
	 * Indicates that the 'top' model can be stacked on top of the 'bottom' model
	 */
	def appendStackable(modelNameBottom: String, modelNameTop: String) {
		planningInitialState += strips.Literal(true, "stackable", modelNameBottom, modelNameTop)
	}
	
	/**
	 * Indicates that the 'top' model can be stacked on top of the 'bottom' model
	 */
	def appendStackables(modelNameBottom: String, modelNameTop_l: Iterable[String]) {
		modelNameTop_l.foreach { modelNameTop =>
			planningInitialState += strips.Literal(true, "stackable", modelNameBottom, modelNameTop)
		}
	}
	
	/**
	 * Indicates that given device can handle the given model
	 */
	def appendDeviceModel(deviceName: String, modelName: String) {
		planningInitialState += strips.Literal(true, "device-can-model", deviceName, modelName)
	}
	
	/**
	 * Indicates that given device can handle the given site
	 */
	def appendDeviceSite(deviceName: String, siteName: String) {
		planningInitialState += strips.Literal(true, "device-can-site", deviceName, siteName)
	}
	
	/**
	 * Indicates that transporter can handle the given site using the given program
	 */
	def appendTransporterCan(deviceName: String, siteName: String, programName: String) {
		planningInitialState += strips.Literal(true, "transporter-can", deviceName, siteName, programName)
	}
	
	def setModel(elementName: String, modelName: String) {
		planningInitialState += strips.Literal(true, "model", elementName, modelName)
	}
}

case class ProtocolCommandResult(
	command: RjsValue,
	effects: strips.Literals = strips.Literals.empty,
	validation_l: List[CommandValidation] = Nil
)

class ProtocolDataB(
	val dataA: ProtocolDataA,
	val commandExpansions: Map[String, ProtocolCommandResult]
)

/**
 * What can be done with commands?
 * 
 * Can expand commands that have already properly validated
 * Need to have JSON that passes commands to Roboliq, such that user sets a variable value, and a properly modified JSON is returned
 * How shall we specify what should be planned? And get the planning to happen automatically?
 * Some planning should happen automatically: check possible variable values, put those in the JSON output, and when there's only one choice, automatically choose it
 * 
 */

class ProtocolDataC(
	val dataA: ProtocolDataA,
	val validations: Map[String, List[CommandValidation]]
)

class ProtocolHandler {
	def extractDataA(protocol: RjsProtocol): ProtocolDataA = {
		val object_m: Map[String, RjsValue] =
			protocol.labwares.asInstanceOf[Map[String, RjsValue]] ++
			protocol.substances.asInstanceOf[Map[String, RjsValue]] ++
			protocol.sources.asInstanceOf[Map[String, RjsValue]]
		val objects = RjsMap(object_m)
		val command_l = protocol.commands.zipWithIndex.map { case (rjsval, i) =>
			(i+1).toString -> rjsval
		}
		val n = command_l.size
		val commandOrderingConstraint_l =
			(1 to n).toList.map(i => i.toString :: (if (i < n) List((i+1).toString) else Nil))
		val commandOrder_l =
			(1 to n).toList.map(_.toString)
		val (planningDomainObjects, planningInitialState) = processDataObjects(objects)
		new ProtocolDataA(
			objects = objects,
			commands = RjsMap(command_l.toMap),
			commandOrderingConstraints = commandOrderingConstraint_l,
			commandOrder = commandOrder_l,
			planningDomainObjects = planningDomainObjects,
			planningInitialState = planningInitialState
		)
	}
	
	private def processDataObjects(
		object_m: RjsMap
	): (Map[String, String], strips.Literals) = {
		val objectToType_m = new HashMap[String, String]
		val atom_l = new ArrayBuffer[strips.Atom]
		for ((name, rjsval) <- object_m.map) {
			rjsval match {
				case plate: RjsProtocolLabware =>
					objectToType_m += (name -> "plate")
					atom_l += strips.Atom("labware", Seq(name))
					plate.model_?.foreach(model => atom_l += strips.Atom("model", Seq(name, model)))
					plate.location_?.foreach(location => atom_l += strips.Atom("location", Seq(name, location)))
					ResultE.unit(())
				case _ =>
					ResultE.unit(())
			}
		}
		(objectToType_m.toMap, strips.Literals(atom_l.toList, Nil))
	}
	
	def stepB(
		dataA: ProtocolDataA
	): ResultE[ProtocolDataB] = {
		var state = dataA.planningInitialState
		val idToResult0_m = new HashMap[String, ProtocolCommandResult]
		def step(idToCommand_l: List[(String, RjsValue)]): ResultE[Map[String, ProtocolCommandResult]] = {
			println("step")
			idToCommand_l match {
				case Nil => ResultE.unit(idToResult0_m.toMap)
				case (id, rjsval) :: res =>
					for {
						pair <- expandCommand(id, rjsval, state)
						(res_m, effects) = pair
						_ = state ++= effects
						_ = idToResult0_m ++= res_m
						res <- step(idToCommand_l.tail)
					} yield res
			}
		}
		
		// Convert List[Int] back to String
		val id_l = getCommandOrdering(dataA.commands.map.keys.toList)
		val idToCommand_l = id_l.map(id => id -> dataA.commands.get(id).get)
		for {
			idToResult_m <- step(idToCommand_l)
		} yield {
			new ProtocolDataB(
				dataA = dataA,
				commandExpansions = idToResult_m
			)
		}
	}
	
	/**
	 * Return a command list ordering.
	 * This will filter out any command ids which have already been expanded.
	 */
	private def getCommandOrdering(
		key_l: List[String]
	): List[String] = {
		// First sort the command keys by id
		val l0 = key_l.map(_.split('.').toList.map(_.toInt)).toList.sorted
		
		@tailrec
		def removeParents(l: List[List[Int]], acc_r: List[List[Int]]): List[List[Int]] = {
			l match {
				case Nil => acc_r.reverse
				case a :: rest =>
					val acc_r_~ = rest match {
						// Drop 'a' if 'b' is it's child
						case b :: _ if (a == b.take(a.size)) =>
							acc_r
						case _ =>
							a :: acc_r
					}
					removeParents(rest, acc_r_~)
			}
		}
		
		// Now remove any commands which have already been expanded
		val l1 = removeParents(l0, Nil)
		
		// TODO: sort `l1` stably using dataA.commandOrderingConstraints
		
		// Convert List[Int] back to String
		l1.map(_.mkString("."))
	}
	
	/**
	 * 1) Check that parameters are all provided
	 * 2) Check that preconditions are all met
	 * 3) Expand command
	 * Return tuple of (map of command IDs to expansion results, cumulative effects)
	 */
	private def expandCommand(
		id: String,
		rjsval: RjsValue,
		state: strips.Literals
	): ResultE[(Map[String, ProtocolCommandResult], strips.Literals)] = {
		println(s"expandCommand($id, $rjsval)")
		val result_m = new HashMap[String, ProtocolCommandResult]
		ResultE.context(s"expandCommand($id)") {
			ResultE.evaluate(rjsval).flatMap {
				case action: RjsAction =>
					expandAction(id, action, state)
				case instruction: RjsInstruction =>
					ResultE.unit((
						Map(id -> ProtocolCommandResult(instruction)),
						strips.Literals.empty
					))
				case RjsNull =>
					ResultE.unit((
						Map(id -> ProtocolCommandResult(RjsNull)),
						strips.Literals.empty
					))
				case _ =>
					// TODO: should perhaps log a warning here instead
					ResultE.error(s"don't know how to expand command: $rjsval")
			}
		}
	}
	
	private def checkActionInput(
		action: RjsAction,
		actionDef: RjsActionDef
	): List[CommandValidation] = {
		// Check that all parameters are provided
		actionDef.params.toList.flatMap { case (name, param) =>
			action.input.get(name) match {
				// If it's missing, return a validation object noting that problem
				case None =>
					Some(CommandValidation_Param(name))
				// Otherwise, we have a value, so return nothing
				case Some(jsvalInput) =>
					None
			}
		}
	}
	
	private def checkActionPreconds(
		action: RjsAction,
		actionDef: RjsActionDef,
		state0: strips.Literals
	): ResultE[List[CommandValidation]] = {
		for {
			precond_l <- bindActionLogic(actionDef.preconds, true, action, actionDef)
		} yield {
			precond_l.flatMap { precond => 
				if (!state0.holds(precond)) {
					Some(CommandValidation_Precond(precond.toString))
				}
				else {
					None
				}
			}
		}
	}
	
	private def getActionEffects(
		action: RjsAction,
		actionDef: RjsActionDef,
		state0: strips.State
	): ResultE[strips.Literals] = {
		for {
			effect_l <- bindActionLogic(actionDef.effects, false, action, actionDef)
		} yield strips.Literals(Unique(effect_l : _*))
	}
	
	/**
	 * Substitute action input into the list of literals, replace $vars with the corresponding input values.
	 */
	private def bindActionLogic(
		literal_l: List[strips.Literal],
		isPrecond: Boolean,
		action: RjsAction,
		actionDef: RjsActionDef
	): ResultE[List[strips.Literal]] = {
		ResultE.mapAll(literal_l) { literal =>
			for {
				binding0_l <- ResultE.mapAll(literal.atom.params) { s =>
					if (s.startsWith("$")) {
						val name = s.tail
						(actionDef.params.get(name), action.input.get(name)) match {
							case (Some(param), Some(jsParam)) =>
								RjsConverter.fromRjs[String](jsParam).map(s2 => Some(s -> s2))
							case (None, _) =>
								ResultE.error(s"invalid parameter `$s` in ${if (isPrecond) "precondition" else "effect"}: $literal")
							case (_, None) =>
								// Don't need to produce an error here, because the missing input will have already been noted while checking the inputs
								ResultE.unit(None)
						}
					}
					else {
						ResultE.unit(None)
					}
				}
			} yield {
				val bindings = binding0_l.flatten.toList.toMap
				literal.bind(bindings)
			}
		}
	}
	
	/**
	 * 1) check inputs
	 * 2) check preconditions
	 * 3) if no errors:
	 * 3.1) evaluate actionDef's 'value' field
	 * 3.2) extract list of child commands
	 * 3.3) call expandAction() on child commands
	 * 3.4) accumulate and return results
	 * 4) else:
	 * 4.1) return errors and effects
	 */
	private def expandAction(
		id: String,
		action: RjsAction,
		state0: strips.Literals
	): ResultE[(Map[String, ProtocolCommandResult], strips.Literals)] = {
		println(s"expandAction($id)")
		val validation_l = new ArrayBuffer[CommandValidation]
		//val child_m = new HashMap[String, RjsValue]
		var effectsCumulative = strips.Literals(Unique[strips.Literal]())
		val result_m = new HashMap[String, ProtocolCommandResult]
		for {
			actionDef <- ResultE.fromScope[RjsActionDef](action.name)
			// Check inputs
			_ = validation_l ++= checkActionInput(action, actionDef)
			// Check that all preconditions are fulfilled
			validation2_l <- checkActionPreconds(action, actionDef, state0)
			_ = validation_l ++= validation2_l
			effects <- bindActionLogic(actionDef.effects, false, action, actionDef)
			_ <- {
				if (!validation_l.isEmpty) {
					ResultE.unit(())
				}
				// If there were no input or precond errors
				else {
					// TODO: we should start a clean scope that only has commandInput_m variables
					ResultE.scope {
						for {
							_ <- ResultE.addToScope(action.input)
							// evaluate actionDef's 'value' field
							// extract list of child commands
							res <- ResultE.evaluate(actionDef.value).flatMap {
								case RjsNull =>
									val idChild = id + ".0"
									//child_m(idChild) = RjsNull
									ResultE.unit(())
								case RjsList(Nil) =>
									val idChild = id + ".0"
									//child_m(idChild) = RjsNull
									ResultE.unit(())
								case RjsList(l) =>
									ResultE.foreach(l.zipWithIndex) { case (rjsChild, i) =>
										val idChild = s"$id.${i+1}"
										//child_m(idChild) = rjsChild
										var state = state0
										for {
											pair <- expandCommand(idChild, rjsChild, state)
										} yield {
											val (resChild_m, effectsChild) = pair
											result_m ++= resChild_m
											effectsCumulative ++= effectsChild
											state ++= effectsChild
										}
									}
								case res =>
									ResultE.error(s"actionDef `${action.name}` should either return `null` or a list of commands.  Actual result: "+res)
							}
						} yield res
					}
				}
			}
		} yield {
			effectsCumulative ++= strips.Literals(Unique(effects : _*))
			result_m(id) = ProtocolCommandResult(
				action,
				effects = strips.Literals(Unique(effects : _*)),
				validation_l = validation_l.toList
			)
			(result_m.toMap, effectsCumulative)
		}
	}
}
