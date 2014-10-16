package roboliq.evoware.translator

import roboliq.commands.DeviceSiteClose
import roboliq.commands.DeviceSiteOpen
import roboliq.input.Context
import roboliq.input.Instruction
import roboliq.commands.ReaderRun
import org.apache.commons.io.FileUtils
import roboliq.evoware.parser.CarrierSite
import roboliq.evoware.parser.EvowareLabwareModel
import roboliq.entities.Labware
import roboliq.entities.Site

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

class EvowareInfiniteM200InstructionHandler(carrierE: roboliq.evoware.parser.Carrier) extends EvowareDeviceInstructionHandler {
	import EvowareDeviceInstructionHandler._
	
	def handleInstruction(
		instruction: Instruction,
		identToAgentObject_m: Map[String, Object]
	): Context[List[TranslationItem]] = {
		for {
			deviceName <- Context.from(carrierE.deviceName_?, s"Evoware device name missing for carrier `${carrierE.sName}`")
			l <- instruction match {
				case DeviceSiteClose(_, _) =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_Close", ""), Nil)))
				case DeviceSiteOpen(_, _) =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_Open", ""), Nil)))
				case cmd: ReaderRun =>
					readerRun_InfiniteM200(identToAgentObject_m, cmd, deviceName)
			}
		} yield l
	}

	private def readerRun_InfiniteM200(
		identToAgentObject_m: Map[String, Object],
		cmd: ReaderRun,
		deviceName: String
	): Context[List[TranslationItem]] = {
		for {
			model <- cmd.labwareToSite_l match {
				case (labware, site) :: Nil => Context.getLabwareModel(labware)
				case _ => Context.error("must supply exactly one labware to seal")
			}
			// List of site/labware mappings for those labware and sites which evoware has equivalences for
			siteToModel_l <- Context.mapFirst(cmd.labwareToSite_l) { case (labware, site) =>
				for {
					labwareIdent <- Context.getEntityIdent(labware)
					siteIdent <- Context.getEntityIdent(site)
					siteToModel <- siteLabwareEntry(identToAgentObject_m, siteIdent, labwareIdent)
				} yield siteToModel
			}
			// Read in the mdfx file, which should be an XML file. 
			programData0 = FileUtils.readFileToString(cmd.programFile)
			// Check for the root XML element
			start_i = programData0.indexOf("<TecanFile")
			_ <- Context.assert(start_i >= 0, s"program file does not have expected contents: $cmd.progfile")
		} yield {
			val programData = programData0.substring(start_i).
				replace("\r", "").
				replace("\n", "").
				replace("&", "&amp;"). // "&amp;" is probably not needed, since I didn't find it in the XML files
				replace("=", "&equal;").
				replace("\"", "&quote;").
				replace("~", "&tilde;").
				replaceAll(">[ \t]+<", "><")
			// Token
			val token = L0C_Facts(deviceName, deviceName+"_Measure", cmd.outputFilename + "|" + programData)
			// Return
			val item = TranslationItem(token, siteToModel_l.flatten)
			List(item)
		}
	}

}