package roboliq.commands

import scala.Option.option2Iterable

import aiplan.strips2.Strips
import aiplan.strips2.Unique
import roboliq.core.RqError
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.entities.Agent
import roboliq.entities.EntityBase
import roboliq.entities.Labware
import roboliq.entities.Shaker
import roboliq.entities.ShakerSpec
import roboliq.entities.Site
import roboliq.entities.WorldState
import roboliq.input.AgentInstruction
import roboliq.input.Context
import roboliq.input.Converter
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue


case class MeasureAbsorbanceActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	programFile_? : Option[String],
	`object`: Labware,
	site_? : Option[Site]
)

class MeasureAbsorbanceActionHandler extends ActionHandler {
	
	def getActionName = "measureAbsorbance"

	def getActionParamNames = List("agent", "device", "programFile", "object", "site")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[MeasureAbsorbanceActionParams](paramToJsval_l, eb, state0)
			labwareName <- eb.getIdent(params.`object`)
			siteName_? <- params.site_? match {
				case None => RqSuccess(None)
				case Some(site) => eb.getIdent(site).map(Some(_))
			}
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent"),
				"?device" -> params.device_?.getOrElse("?device"),
				"?labware" -> labwareName,
				"?site" -> siteName_?.getOrElse("?site")
			)
			val binding = binding_l.toMap

			List(
				OperatorInfo(id ++ List(1), Nil, Nil, "openDeviceSite", binding, Map()),
				OperatorInfo(id ++ List(2), Nil, Nil, "transportLabware", binding, Map()),
				OperatorInfo(id ++ List(3), Nil, Nil, "closeDeviceSite", binding, Map()),
				OperatorInfo(id ++ List(4), Nil, Nil, "measureAbsorbance", binding, paramToJsval_l.toMap),
				OperatorInfo(id ++ List(5), Nil, Nil, "openDeviceSite", binding, Map()),
				OperatorInfo(id ++ List(6), Nil, Nil, "transportLabware", binding, Map()),
				OperatorInfo(id ++ List(7), Nil, Nil, "closeDeviceSite", binding, Map())
			)
		}
	}
}

class MeasureAbsorbanceOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "measureAbsorbance",
			paramName_l = List("?agent", "?device", "?labware", "?model", "?site"),
			paramTyp_l = List("agent", "reader", "labware", "model", "site"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "agent-has-device", "?agent", "?device"),
				Strips.Literal(Strips.Atom("device-can-site", List("?device", "?site")), true),
				Strips.Literal(Strips.Atom("location", List("?labware", "?site")), true)
				// TODO: device site should be closed
			)),
			effects = aiplan.strips2.Strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(agentName, deviceName, labwareName, _, siteName) = operator.paramName_l
		
		for {
			agent <- Context.getEntityAs[Agent](agentName)
			device <- Context.getEntityAs[Shaker](deviceName)
			program <- instructionParam_m.get("program") match {
				case Some(x@JsObject(obj)) =>
					Context.getEntityAs[ShakerSpec](x)
				case Some(JsString(s)) =>
					val programName = operator.paramName_l(2)
					Context.getEntityAs[ShakerSpec](programName)
				case _ => Context.error("Expected identifier or shaker program")
			}
			labware <- Context.getEntityAs[Labware](labwareName)
			site <- Context.getEntityAs[Site](siteName)
			instruction = ShakerRun(
				device,
				program,
				List((labware, site))
			)
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
