package roboliq.input

import spray.json.JsObject
import spray.json.JsValue
import scala.collection.mutable.ArrayBuffer
import roboliq.ai.strips
import scala.collection.mutable.HashMap
import scala.math.Ordering.Implicits._
import scala.annotation.tailrec
import roboliq.ai.plan.Unique

sealed trait CommandValidation
case class CommandValidation_Param(name: String) extends CommandValidation
case class CommandValidation_Precond(description: String) extends CommandValidation

case class MyPlate(
	model_? : Option[String],
	location_? : Option[String]
)

class Protocol2DataA(
	val objects: RjsMap,
	val commands: RjsMap,
	val commandOrderingConstraints: List[List[String]],
	val commandOrder: List[String],
	val planningDomainObjects: Map[String, String],
	val planningInitialState: strips.Literals
) {
	def ++(that: Protocol2DataA): Protocol2DataA = {
		new Protocol2DataA(
			objects = this.objects ++ that.objects,
			commands = this.commands ++ that.commands,
			commandOrderingConstraints = this.commandOrderingConstraints ++ that.commandOrderingConstraints,
			commandOrder = this.commandOrder ++ that.commandOrder,
			planningDomainObjects = this.planningDomainObjects ++ that.planningDomainObjects,
			planningInitialState = this.planningInitialState ++ that.planningInitialState
		)
	}
}

case class ProtocolCommandResult(
	effects: strips.Literals,
	validation_l: List[CommandValidation]
)

class Protocol2DataB(
	val dataA: Protocol2DataA,
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

class Protocol2DataC(
	val dataA: Protocol2DataA,
	val validations: Map[String, List[CommandValidation]]
)

class Protocol2 {
	def extractDataA(protocol: RjsProtocol): Protocol2DataA = {
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
		new Protocol2DataA(
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
					ContextE.unit(())
				case _ =>
					ContextE.unit(())
			}
		}
		(objectToType_m.toMap, strips.Literals(atom_l.toList, Nil))
	}
	
	def stepB(
		dataA: Protocol2DataA
	): ContextE[Protocol2DataB] = {
		var state = dataA.planningInitialState
		val idToResult0_m = new HashMap[String, ProtocolCommandResult]
		def step(idToCommand_l: List[(String, RjsValue)]): ContextE[Map[String, ProtocolCommandResult]] = {
			idToCommand_l match {
				case Nil => ContextE.unit(idToResult0_m.toMap)
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
			new Protocol2DataB(
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
	): ContextE[(Map[String, ProtocolCommandResult], strips.Literals)] = {
		val result_m = new HashMap[String, ProtocolCommandResult]
		ContextE.context(s"expandCommand($id)") {
			ContextE.evaluate(rjsval).flatMap {
				case RjsNull =>
					ContextE.unit((Map(), strips.Literals.empty))
				case action: RjsAction =>
					expandAction(id, action, state)
				case instruction: RjsInstruction =>
					ContextE.unit((Map(), strips.Literals.empty))
				case _ =>
					// TODO: should perhaps log a warning here instead
					ContextE.error(s"don't know how to expand command: $rjsval")
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
	): ContextE[List[CommandValidation]] = {
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
	): ContextE[strips.Literals] = {
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
	): ContextE[List[strips.Literal]] = {
		ContextE.mapAll(literal_l) { literal =>
			for {
				binding0_l <- ContextE.mapAll(literal.atom.params) { s =>
					if (s.startsWith("$")) {
						val name = s.tail
						(actionDef.params.get(name), action.input.get(name)) match {
							case (Some(param), Some(jsParam)) =>
								ContextE.fromRjs[String](jsParam).map(s2 => Some(s -> s2))
							case (None, _) =>
								ContextE.error(s"invalid parameter `$s` in ${if (isPrecond) "precondition" else "effect"}: $literal")
							case (_, None) =>
								// Don't need to produce an error here, because the missing input will have already been noted while checking the inputs
								ContextE.unit(None)
						}
					}
					else {
						ContextE.unit(None)
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
	): ContextE[(Map[String, ProtocolCommandResult], strips.Literals)] = {
		val validation_l = new ArrayBuffer[CommandValidation]
		val child_m = new HashMap[String, RjsValue]
		var effectsCumulative = strips.Literals(Unique[strips.Literal]())
		val result_m = new HashMap[String, ProtocolCommandResult]
		for {
			actionDef <- ContextE.fromScope[RjsActionDef](action.name)
			// Check inputs
			_ = validation_l ++= checkActionInput(action, actionDef)
			// Check that all preconditions are fulfilled
			validation2_l <- checkActionPreconds(action, actionDef, state0)
			_ = validation_l ++= validation2_l
			effects <- bindActionLogic(actionDef.effects, false, action, actionDef)
			_ <- {
				if (!validation_l.isEmpty) {
					ContextE.unit(())
				}
				// If there were no input or precond errors
				else {
					// TODO: we should start a clean scope that only has commandInput_m variables
					ContextE.scope {
						for {
							_ <- ContextE.addToScope(action.input)
							// evaluate actionDef's 'value' field
							// extract list of child commands
							res <- ContextE.evaluate(actionDef.value).map {
								case RjsNull =>
									val idChild = id + ".0"
									child_m(idChild) = RjsNull
									ContextE.unit(())
								case RjsList(Nil) =>
									val idChild = id + ".0"
									child_m(idChild) = RjsNull
									ContextE.unit(())
								case RjsList(l) =>
									ContextE.foreach(l.zipWithIndex) { case (rjsChild, i) =>
										val idChild = s"$id.${i+1}"
										child_m(idChild) = rjsChild
										var state = state0
										for {
											pair <- expandCommand(idChild, rjsChild, state)
										} yield {
											val (resChild_m, effectsChild) = pair
											result_m ++= resChild_m
											effectsCumulative ++= effectsChild
											state = state ++ effectsChild
										}
									}
								case res =>
									ContextE.error(s"actionDef `${action.name}` should either return `null` or a list of commands.  Actual result: "+res)
							}
						} yield res
					}
				}
			}
		} yield {
			effectsCumulative ++= strips.Literals(Unique(effects : _*))
			result_m(id) = ProtocolCommandResult(
				effects = strips.Literals(Unique(effects : _*)),
				validation_l = validation_l.toList
			)
			(result_m.toMap, effectsCumulative)
		}
	}
}
