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

class Protocol2 {
	def processData(
		data: RjsMap
	): ContextE[(Map[String, String], Strips.State)] = {
		processDataObjects(data)
	}
	
	def processDataObjects(
		m: RjsMap
	): ContextE[(Map[String, String], Strips.State)] = {
		val objectToType_m = new HashMap[String, String]
		val atom_l = new ArrayBuffer[Strips.Atom]
		ContextE.context("object") {
			for {
				object_m <- ContextE.fromRjs[RjsMap](m, "object")
				_ <- ContextE.mapAll(object_m.map.toList) { case (name, jsval) =>
					ContextE.context(name) {
						for {
							m <- ContextE.fromRjs[Map[String, RjsValue]](jsval)
							typ <- ContextE.fromRjs[String](m, "type")
							_ <- typ match {
								case "plate" =>
									for {
										plate <- ContextE.fromRjs[MyPlate](jsval)
									} yield {
										objectToType_m += (name -> typ)
										atom_l += Strips.Atom("labware", Seq(name))
										plate.model_?.foreach(model => atom_l += Strips.Atom("model", Seq(name, model)))
										plate.location_?.foreach(location => atom_l += Strips.Atom("location", Seq(name, location)))
									}
								case _ =>
									ContextE.unit(())
							}
						} yield ()
					}
				}
			} yield {
				(objectToType_m.toMap, Strips.State(atom_l.toSet))
			}
		}
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

	/*
	def evaluateDataCommand(state: Strips.State, data: JsObject, id: String): ContextE[JsObject] = {
		ContextE.context(s"command[$id]") {
			for {
				jsCommands <- ContextE.fromRjs[JsObject](data.fields, "command")
				jsCommandCall <- ContextE.fromRjs[JsObject](jsCommands.fields, id)
				_ = println(s"command_m: ${jsCommandCall}")
				commandName <- ContextE.fromRjs[String](jsCommandCall.fields, "command")
				commandDef <- lookupCommandDef(commandName)
				commandInput_m <- ContextE.fromRjs[Map[String, JsValue]](jsCommandCall.fields, "input")
				res <- ContextE.scope {
					for {
						_ <- ContextE.addToScope(commandInput_m)
						res <- ContextE.evaluate(commandDef.output)
					} res
				}
			} yield {
				validation_l.toList
			}
		}
	}*/
}
