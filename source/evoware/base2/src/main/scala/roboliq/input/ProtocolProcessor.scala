/*package roboliq.input

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.math.Ordering.Implicits.seqDerivedOrdering
import scala.reflect.runtime.universe

import roboliq.ai.plan.Unique
import roboliq.ai.strips
import roboliq.ai.strips._
import roboliq.core.ResultC

/**
 * ProtocolProcessor is an object with a method to extract ProtocolData from
 * a high-level protocol description.
 */
object ProtocolProcessor {
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
	/*
	def expandCommands(
		details0: ProtocolData
	): ResultE[ProtocolData] = {
		var state = details0.planningInitialState
		val idToInfo0_m = new HashMap[String, CommandInfo]
		def step(idToCommand_l: List[(String, RjsValue)]): ResultE[Map[String, CommandInfo]] = {
			println("step")
			idToCommand_l match {
				case Nil => ResultE.unit(idToInfo0_m.toMap)
				case (id, rjsval) :: res =>
					for {
						pair <- expandCommand(id, rjsval, state)
						(res_m, effects) = pair
						_ = state ++= effects
						_ = idToInfo0_m ++= res_m
						res <- step(idToCommand_l.tail)
					} yield res
			}
		}
		
		// Convert List[Int] back to String
		val id_l = getCommandOrdering(details0.commands.keys.toList)
		val idToCommand_l = id_l.map(id => id -> details0.commands(id).command)
		for {
			idToInfo_m <- step(idToCommand_l)
		} yield {
			details0.copy(
				commands = idToInfo_m
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
	): ResultE[(Map[String, CommandInfo], strips.Literals)] = {
		println(s"expandCommand($id, $rjsval)")
		val result_m = new HashMap[String, CommandInfo]
		ResultE.context(s"expandCommand($id)") {
			ResultE.evaluate(rjsval).flatMap {
				case action: RjsAction =>
					expandAction(id, action, state)
				case instruction: RjsInstruction =>
					ResultE.unit((
						Map(id -> CommandInfo(instruction)),
						strips.Literals.empty
					))
				case RjsNull =>
					ResultE.unit((
						Map(id -> CommandInfo(RjsNull)),
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
					Some(CommandValidation("missing parameter value", param_? = Some(name)))
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
			precond_l.zipWithIndex flatMap { case (precond, i) => 
				if (!state0.holds(precond)) {
					Some(CommandValidation(precond.toString, precond_? = Some(i+1)))
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
	): ResultE[(Map[String, CommandInfo], strips.Literals)] = {
		println(s"expandAction($id)")
		val validation_l = new ArrayBuffer[CommandValidation]
		//val child_m = new HashMap[String, RjsValue]
		var effectsCumulative = strips.Literals(Unique[strips.Literal]())
		val info_m = new HashMap[String, CommandInfo]
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
											info_m ++= resChild_m
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
			info_m(id) = CommandInfo(
				action,
				effects = strips.Literals(Unique(effects : _*)),
				validations = validation_l.toList
			)
			(info_m.toMap, effectsCumulative)
		}
	}
	*/
}
*/