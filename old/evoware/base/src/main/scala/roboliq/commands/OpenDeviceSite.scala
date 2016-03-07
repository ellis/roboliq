package roboliq.commands

import scala.reflect.runtime.universe
import roboliq.ai.strips
import roboliq.ai.plan.Unique
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.entities.Agent
import roboliq.entities.EntityBase
import roboliq.entities.Reader
import roboliq.entities.Site
import roboliq.entities.WorldState
import roboliq.input.Context
import roboliq.input.Converter
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue
import roboliq.entities.Device


case class OpenDeviceSiteActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	site_? : Option[Site]
)

class OpenDeviceSiteActionHandler extends ActionHandler {
	
	def getActionName = "openDeviceSite"

	def getActionParamNames = List("agent", "device", "site")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[OpenDeviceSiteActionParams](paramToJsval_l, eb, state0)
			siteName_? <- params.site_? match {
				case None => RqSuccess(None)
				case Some(site) => eb.getIdent(site).map(Some(_))
			}
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent"),
				"?device" -> params.device_?.getOrElse("?device"),
				"?site" -> siteName_?.getOrElse("?site")
			)
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, "openDeviceSite", binding, paramToJsval_l.toMap) :: Nil
		}
	}
}

class OpenDeviceSiteOperatorHandler extends OperatorHandler {
	def getDomainOperator: strips.Operator = {
		strips.Operator(
			name = "openDeviceSite",
			paramName_l = List("?agent", "?device", "?site"),
			paramTyp_l = List("agent", "device", "site"),
			preconds = strips.Literals(Unique(
				strips.Literal(true, "agent-has-device", "?agent", "?device"),
				strips.Literal(true, "device-can-open-site", "?device", "?site")
			)),
			effects = strips.Literals(Unique(
				strips.Literal(false, "site-closed", "?site")
			))
		)
	}
	
	def getInstruction(
		operator: strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(agentName, deviceName, siteName) = operator.paramName_l
		
		for {
			agent <- Context.getEntityAs[Agent](agentName)
			device <- Context.getEntityAs[Device](deviceName)
			site <- Context.getEntityAs[Site](siteName)
			instruction = DeviceSiteOpen(
				device,
				site
			)
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
