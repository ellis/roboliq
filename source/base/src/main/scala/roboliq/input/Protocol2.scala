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
		data: JsObject
	): ContextE[(Map[String, String], Strips.State)] = {
		val m = data.fields
		processDataObjects(m)
	}
	
	def processDataObjects(
		m: Map[String, JsValue]
	): ContextE[(Map[String, String], Strips.State)] = {
		val objectToType_m = new HashMap[String, String]
		val atom_l = new ArrayBuffer[Strips.Atom]
		ContextE.context("object") {
			for {
				object_m <- ContextE.fromJson[Map[String, JsValue]](m, "object")
				_ <- ContextE.mapAll(object_m.toList) { case (name, jsval) =>
					ContextE.context(name) {
						for {
							m <- ContextE.fromJson[Map[String, JsValue]](jsval)
							typ <- ContextE.fromJson[String](m, "type")
							_ <- typ match {
								case "plate" =>
									for {
										plate <- ContextE.fromJson[MyPlate](jsval)
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
	
	def validateDataCommand(data: JsObject, id: String): ContextE[List[CommandValidation]] = {
		val validation_l = new ArrayBuffer[CommandValidation]
		ContextE.context(s"command[$id]") {
			for {
				m <- ContextE.fromJson[Map[String, JsValue]](data.fields, id)
				commandName <- ContextE.fromJson[String](m, "command")
				commandDef <- lookupCommandDef(commandName)
				_ <- ContextE.foreach(commandDef.param) { param =>
					m.get(param.name) match {
						case None =>
							validation_l += CommandValidation_Param(param.name)
						case Some(jsvalInput) =>
					}
					ContextE.unit(())
				}
			} yield {
				validation_l.toList
			}
		}
	}
	
	/*
	 *     TYPE: action
    PARAM:
    - { name: agent, type: Agent, input: Plannable }
    - { name: device, type: Device, input: Plannable }
    - { name: program, type: Program, input: Required }
    - { name: object, type: Labware, input: Required }
    - { name: site, type: Site, input: Plannable }
    PRECOND:
    - agent-has-device $agent $device
    - device-can-site $device $site
    - model $labware $model
    - location $labware $site
    OUTPUT:
    - TYPE: instruction
      NAME: ShakerRun
      INPUT: { agent: $agent, device: $device, program: $program, labware: $object, site: $site }
	 * 
	 */
	def jsToActionDef(name: String, jsobj: JsObject): ContextE[ActionDef] = {
		val m = jsobj.fields
		for {
			typ <- ContextE.fromJson[String](m, "TYPE")
			param_l <- ContextE.fromJson[List[InputDef]](m, "PARAM")
			precond0_l <- ContextE.fromJson[List[String]](m, "PRECOND")
			precond_l = precond0_l.map(s => Strips.Literal.parse(s))
			jsOutput <- ContextE.fromJson[JsValue](m, "OUTPUT")
		} yield {
			ActionDef(
				name = name,
				description_? = None,
				documentation_? = None,
				param = param_l,
				precond = precond_l,
				effect = Nil,
				output = jsOutput
			)
		}
		
	}
	
	def lookupCommandDef(name: String): ContextE[ActionDef] = {
		for {
			jsobj <- ContextE.getScopeValue(name)
			actionDef <- jsToActionDef(name, jsobj)
		} yield actionDef
	}
}
