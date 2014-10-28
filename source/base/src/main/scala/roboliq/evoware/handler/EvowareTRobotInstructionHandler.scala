package roboliq.evoware.handler

import roboliq.commands.ReaderRun
import roboliq.evoware.translator.L0C_Facts
import roboliq.commands.DeviceSiteOpen
import roboliq.commands.DeviceClose
import roboliq.commands.DeviceSiteClose
import roboliq.evoware.translator.TranslationItem
import org.apache.commons.io.FileUtils
import roboliq.input.Context
import roboliq.input.Instruction


class EvowareTRobotInstructionHandler(carrierE: roboliq.evoware.parser.Carrier) extends EvowareDeviceInstructionHandler {
	import EvowareDeviceInstructionHandler._
	
	def handleInstruction(
		instruction: Instruction,
		identToAgentObject_m: Map[String, Object]
	): Context[List[TranslationItem]] = {
		for {
			deviceName <- Context.from(carrierE.deviceName_?, s"Evoware device name missing for carrier `${carrierE.sName}`")
			l <- instruction match {
				case _: DeviceClose =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_LidClose", ""), Nil)))
				case _: DeviceSiteClose =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_LidClose", ""), Nil)))
				case _: DeviceSiteOpen =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_LidOpen", ""), Nil)))
				case cmd: ReaderRun =>
					run(identToAgentObject_m, cmd, deviceName)
			}
		} yield l
	}

	private def run(
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
