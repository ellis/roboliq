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


case class CarouselOpenSiteActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	site: Site
)

class CarouselOpenSiteActionHandler extends ActionHandler {
	
	def getActionName = "carousel.openSite"

	def getActionParamNames = List("agent", "device", "site")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[CarouselOpenSiteActionParams](paramToJsval_l, eb, state0)
			siteIdent <- eb.getIdent(params.site)
			i = siteIdent.lastIndexOf("_")
			_ <- RsResult.assert(i > 0, s"couldn't extract carousel position from site name `$siteIdent`")
		} yield {
			val num = siteIdent.substring(i + 1)
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent"),
				"?device" -> params.device_?.getOrElse("?device"),
				"?site" -> siteIdent
			)
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, "carousel.openSite-"+siteIdent, binding, paramToJsval_l.toMap) :: Nil
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
class CarouselOpenSiteOperatorHandler(
	agentIdent: String,
	deviceIdent: String,
	internalSiteIdent: String,
	internalSiteIdent_l: List[String]
) extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "carousel.openSite-"+internalSiteIdent, // The `id` refers to an internal site
			paramName_l = Nil, // This is the external site on the robot bench, not one of the internal sites.
			paramTyp_l = Nil,
			preconds = Strips.Literals(Unique()),
			effects = Strips.Literals(Unique(internalSiteIdent_l.map(ident => 
				Strips.Literal(ident != internalSiteIdent, "site-closed", ident)
			) : _*))
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		for {
			agent <- Context.getEntityAs[Agent](agentIdent)
			device <- Context.getEntityAs[Device](deviceIdent)
			site <- Context.getEntityAs[Site](internalSiteIdent)

			// TODO: Extracting the position from the siteIdent is very awkward -- attach information to the site in some more direct way
			siteIdent = site.label.get
			i = siteIdent.lastIndexOf("_")
			_ <- Context.assert(i > 0, s"couldn't extract carousel position from site name `$siteIdent`")
			num = siteIdent.substring(i + 1)
			
			// Carousel rotate to ident
			_ <- Context.addInstruction(agent, DeviceCarouselMoveTo(device, num))
			// Open the external site
			_ <- Context.addInstruction(agent, DeviceSiteOpen(device, site))
		} yield ()
	}
}
