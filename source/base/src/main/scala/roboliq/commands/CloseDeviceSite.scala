package roboliq.commands

import scala.reflect.runtime.universe

import aiplan.strips2.Strips
import aiplan.strips2.Unique
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


case class CloseDeviceSiteActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	site_? : Option[Site]
)

// REFACTOR: This entire file is practically a duplicate of OpenDeviceSite.scala
class CloseDeviceSiteActionHandler extends ActionHandler {
	
	def getActionName = "closeDeviceSite"

	def getActionParamNames = List("agent", "device", "site")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[CloseDeviceSiteActionParams](paramToJsval_l, eb, state0)
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

			OperatorInfo(id, Nil, Nil, "closeDeviceSite", binding, paramToJsval_l.toMap) :: Nil
		}
	}
}

class CloseDeviceSiteOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "closeDeviceSite",
			paramName_l = List("?agent", "?device", "?site"),
			paramTyp_l = List("agent", "device", "site"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "agent-has-device", "?agent", "?device"),
				Strips.Literal(true, "device-can-open-site", "?device", "?site")
			)),
			effects = Strips.Literals(Unique(
				Strips.Literal(false, "is-site-closed", "?site")
			))
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(agentName, deviceName, siteName) = operator.paramName_l
		
		for {
			agent <- Context.getEntityAs[Agent](agentName)
			device <- Context.getEntityAs[Reader](deviceName)
			site <- Context.getEntityAs[Site](siteName)
			instruction = DeviceSiteClose(
				device,
				site
			)
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
