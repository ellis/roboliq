package roboliq.input

import spray.json.JsObject
import spray.json.JsValue
import scala.collection.mutable.ArrayBuffer
import aiplan.strips2.Strips
import scala.collection.mutable.HashMap

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
	val planningProblemState: Strips.State
)

class Protocol2DataB(
	val dataA: Protocol2DataA,
	val validations: Map[String, CommandValidation]
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
		val (planningDomainObjects, planningProblemState) = processDataObjects(objects)
		new Protocol2DataA(
			objects = objects,
			commands = RjsMap(command_l.toMap),
			commandOrderingConstraints = commandOrderingConstraint_l,
			commandOrder = commandOrder_l,
			planningDomainObjects = planningDomainObjects,
			planningProblemState = planningProblemState
		)
	}
	
	private def processDataObjects(
		object_m: RjsMap
	): (Map[String, String], Strips.State) = {
		val objectToType_m = new HashMap[String, String]
		val atom_l = new ArrayBuffer[Strips.Atom]
		for ((name, rjsval) <- object_m.map) {
			rjsval match {
				case plate: RjsProtocolLabware =>
					objectToType_m += (name -> "plate")
					atom_l += Strips.Atom("labware", Seq(name))
					plate.model_?.foreach(model => atom_l += Strips.Atom("model", Seq(name, model)))
					plate.location_?.foreach(location => atom_l += Strips.Atom("location", Seq(name, location)))
					ContextE.unit(())
				case _ =>
					ContextE.unit(())
			}
		}
		(objectToType_m.toMap, Strips.State(atom_l.toSet))
	}
	
	def validateDataCommand(
		state: Strips.State,
		data: RjsMap,
		id: String
	): ContextE[List[CommandValidation]] = {
		val validation_l = new ArrayBuffer[CommandValidation]
		ContextE.context(s"command[$id]") {
			for {
				jsCommandSet <- ContextE.fromRjs[RjsMap](data, "command")
				command_m <- ContextE.fromRjs[RjsMap](jsCommandSet, id)
				_ = println(s"command_m: ${command_m}")
				commandName <- ContextE.fromRjs[String](command_m, "command")
				actionDef <- ContextE.fromScope[RjsActionDef](commandName)
				commandInput_m <- ContextE.fromRjs[RjsMap](command_m, "input")
				_ <- ContextE.foreach(actionDef.params) { param =>
					commandInput_m.get(param.name) match {
						case None =>
							validation_l += CommandValidation_Param(param.name)
						case Some(jsvalInput) =>
					}
					ContextE.unit(())
				}
				_ <- {
					if (validation_l.isEmpty) {
						ContextE.foreach(actionDef.preconds) { precond =>
							for {
								binding0_l <- ContextE.mapAll(precond.atom.params) { s =>
									if (s.startsWith("$")) {
										commandInput_m.get(s.tail) match {
											case None =>
												ContextE.error(s"unknown parameter `$s` in precondition $precond")
											case Some(jsParam) =>
												ContextE.fromRjs[String](jsParam).map(s2 => Some(s -> s2))
										}
									}
									else {
										ContextE.unit(None)
									}
								}
							} yield {
								val binding = binding0_l.flatten.toList.toMap
								val precond2 = precond.bind(binding)
								if (state.holds(precond2.atom) != precond2.pos) {
									validation_l += CommandValidation_Precond(precond2.toString)
								}
							}
						}
					}
					else {
						ContextE.unit(())
					}
				}
			} yield {
				validation_l.toList
			}
		}
	}

	def evaluateDataCommand(state: Strips.State, data: RjsMap, id: String): ContextE[RjsValue] = {
		ContextE.context(s"command[$id]") {
			for {
				jsCommandSet <- ContextE.fromRjs[RjsMap](data, "command")
				command_m <- ContextE.fromRjs[RjsMap](jsCommandSet, id)
				_ = println(s"command_m: ${command_m}")
				commandName <- ContextE.fromRjs[String](command_m, "command")
				actionDef <- ContextE.fromScope[RjsActionDef](commandName)
				commandInput_m <- ContextE.fromRjs[RjsMap](command_m, "input")
				// TODO: we should start a clean scope that only has commandInput_m variables
				res <- ContextE.scope {
					for {
						_ <- ContextE.addToScope(commandInput_m)
						res <- ContextE.evaluate(actionDef.value)
					} yield res
				}
			} yield res
		}
	}
}
