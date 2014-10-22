package roboliq.commands

import scala.reflect.runtime.universe

import aiplan.strips2.Strips
import aiplan.strips2.Unique
import roboliq.core.RqResult
import roboliq.core.RsResult
import roboliq.entities.Agent
import roboliq.entities.Device
import roboliq.entities.EntityBase
import roboliq.entities.Site
import roboliq.entities.WorldState
import roboliq.input.Context
import roboliq.input.Converter
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue


case class CarouselCloseActionParams(
	agent_? : Option[String],
	device: Device
)

class CarouselCloseActionHandler extends ActionHandler {
	
	def getActionName = "carousel.close"

	def getActionParamNames = List("agent", "device")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[CarouselCloseActionParams](paramToJsval_l, eb, state0)
			deviceIdent <- eb.getIdent(params.device)
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent")
			)
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, "carousel.close-"+deviceIdent, binding, paramToJsval_l.toMap) :: Nil
		}
	}
}

/**
 * Rotate carousel to internal site 'internalSiteIdent' and open the device.
 * 
 * This operator assumes that there is only one external opening.
 * 
 * The effect are: the specified site is open and the other internal sites are closed.
 */
class CarouselCloseOperatorHandler(
	deviceIdent: String,
	internalSiteIdent_l: List[String]
) extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "carousel.close-"+deviceIdent, // The `id` refers to an internal site
			paramName_l = List("?agent"), // This is the external site on the robot bench, not one of the internal sites.
			paramTyp_l = List("agent"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "agent-has-device", "?agent", deviceIdent)
			)),
			effects = Strips.Literals(Unique(internalSiteIdent_l.map(ident => 
				Strips.Literal(true, "site-closed", ident)
			) : _*))
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(agentName) = operator.paramName_l
		
		for {
			agent <- Context.getEntityAs[Agent](agentName)
			device <- Context.getEntityAs[Device](deviceIdent)

			// Close the device
			_ <- Context.addInstruction(agent, DeviceClose(device))
		} yield ()
	}
}
