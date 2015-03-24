package roboliq.evoware.handler

import roboliq.entities.Labware
import roboliq.entities.Site
import roboliq.evoware.parser.CarrierSite
import roboliq.evoware.parser.EvowareLabwareModel
import roboliq.evoware.translator.TranslationItem
import roboliq.input.Context
import roboliq.input.Instruction

trait EvowareDeviceInstructionHandler {
	def handleInstruction(
		instruction: Instruction,
		identToAgentObject_m: Map[String, Object]
	): Context[List[TranslationItem]]
}

object EvowareDeviceInstructionHandler {
	
	protected def getAgentObject[A](ident: String, identToAgentObject_m: Map[String, Object], error: => String): Context[A] = {
		Context.from(identToAgentObject_m.get(ident).map(_.asInstanceOf[A]), error)
	}

	def siteLabwareEntry(
		identToAgentObject_m: Map[String, Object],
		siteIdent: String,
		labwareIdent: String
	): Context[Option[(CarrierSite, EvowareLabwareModel)]] = {
		for {
			labware <- Context.getEntityAs[Labware](labwareIdent)
			model <- Context.getLabwareModel(labware)
			modelIdent <- Context.getEntityIdent(model)
		} yield siteLabwareEntrySub(identToAgentObject_m, siteIdent, modelIdent)
	}
	
	def siteLabwareEntry(
		identToAgentObject_m: Map[String, Object],
		site: Site,
		labware: Labware
	): Context[Option[(CarrierSite, EvowareLabwareModel)]] = {
		for {
			siteIdent <- Context.getEntityIdent(site)
			model <- Context.getLabwareModel(labware)
			modelIdent <- Context.getEntityIdent(model)
		} yield siteLabwareEntrySub(identToAgentObject_m, siteIdent, modelIdent)
	}
	
	private def siteLabwareEntrySub(
		identToAgentObject_m: Map[String, Object],
		siteIdent: String,
		modelIdent: String
	): Option[(CarrierSite, EvowareLabwareModel)] = {
		val modelE_? = identToAgentObject_m.get(modelIdent).map(_.asInstanceOf[EvowareLabwareModel])
		val siteE_? = identToAgentObject_m.get(siteIdent).map(_.asInstanceOf[CarrierSite])

		(siteE_?, modelE_?) match {
			case (Some(siteE), Some(modelE)) => Some(siteE -> modelE)
			case _ => None
		}
	}
	
}
