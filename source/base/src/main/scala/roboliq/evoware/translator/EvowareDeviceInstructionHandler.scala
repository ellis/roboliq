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
		} yield {
			// Need to create something like this:
			// B;FACTS("InfiniteM200","InfiniteM200_Measure","C:\DataExchange\Culti12h_Pre_culture.xml|<TecanFile xmlns:xsi&equal;&quote;http://www.w3.org/2001/XMLSchema-instance&quote; xsi:schemaLocation&equal;&quote;tecan.at.schema.documents Main.xsd&quote; fileformat&equal;&quote;Tecan.At.Measurement&quote; fileversion&equal;&quote;2.0&quote; xmlns&equal;&quote;tecan.at.schema.documents&quote;><FileInfo type&equal;&quote;&quote; instrument&equal;&quote;infinite 200Pro&quote; version&equal;&quote;&quote; createdFrom&equal;&quote;localadmin&quote; createdAt&equal;&quote;2012-10-10T09:55:56.0134925Z&quote; createdWith&equal;&quote;Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor&quote; description&equal;&quote;&quote; /><TecanMeasurement id&equal;&quote;1&quote; class&equal;&quote;Measurement&quote;><MeasurementManualCycle id&equal;&quote;2&quote; number&equal;&quote;1&quote; type&equal;&quote;Standard&quote;><CyclePlate id&equal;&quote;3&quote; file&equal;&quote;COS96rt&quote; plateWithCover&equal;&quote;False&quote;><PlateRange id&equal;&quote;0&quote; range&equal;&quote;A1&tilde;H12&quote; auto&equal;&quote;true&quote;><Temperature id&equal;&quote;4&quote; module&equal;&quote;PLATE&quote; temperature&equal;&quote;370&quote; switch&equal;&quote;ON&quote; /><WaitTemperature id&equal;&quote;5&quote; module&equal;&quote;PLATE&quote; minTemperature&equal;&quote;365&quote; maxTemperature&equal;&quote;375&quote; maxWaitTimeSpan&equal;&quote;P10675199DT2H48M5.4775807S&quote; /><MeasurementAbsorbance id&equal;&quote;8&quote; mode&equal;&quote;Normal&quote; type&equal;&quote;&quote; name&equal;&quote;ABS&quote; longname&equal;&quote;&quote; description&equal;&quote;&quote;><MeasurementKinetic id&equal;&quote;6&quote; loops&equal;&quote;2&quote; timeSpan&equal;&quote;PT0S&quote; maxDeviation&equal;&quote;PT0S&quote; duration&equal;&quote;PT12H&quote; useDuration&equal;&quote;true&quote;><Incubation id&equal;&quote;7&quote; timeSpan&equal;&quote;PT16M3S&quote; maxDeviation&equal;&quote;PT0S&quote; refTimeID&equal;&quote;0&quote; ignoreInLastCycle&equal;&quote;True&quote;><Shaking id&equal;&quote;0&quote; mode&equal;&quote;Orbital&quote; time&equal;&quote;PT16M&quote; frequency&equal;&quote;0&quote; amplitude&equal;&quote;5000&quote; maxDeviation&equal;&quote;PT0S&quote; settleTime&equal;&quote;PT0S&quote; /><RemainingWaitTime id&equal;&quote;0&quote; timeSpan&equal;&quote;PT1M&quote; maxDeviation&equal;&quote;PT0S&quote; refTimeID&equal;&quote;0&quote; ignoreInLastCycle&equal;&quote;False&quote; /></Incubation><Well id&equal;&quote;9&quote; auto&equal;&quote;true&quote;><MeasurementReading id&equal;&quote;10&quote; name&equal;&quote;&quote; refID&equal;&quote;6&quote; refName&equal;&quote;KINETIC.RUN.CYCLE&quote; beamDiameter&equal;&quote;700&quote; beamGridType&equal;&quote;Single&quote; beamGridSize&equal;&quote;1&quote; beamEdgeDistance&equal;&quote;auto&quote;><ReadingLabel id&equal;&quote;11&quote; name&equal;&quote;Label1&quote; scanType&equal;&quote;ScanFixed&quote; refID&equal;&quote;0&quote;><ReadingSettings number&equal;&quote;25&quote; rate&equal;&quote;25000&quote; /><ReadingTime integrationTime&equal;&quote;0&quote; lagTime&equal;&quote;0&quote; readDelay&equal;&quote;0&quote; flash&equal;&quote;0&quote; dark&equal;&quote;0&quote; /><ReadingFilter id&equal;&quote;0&quote; type&equal;&quote;Ex&quote; wavelength&equal;&quote;6000&quote; bandwidth&equal;&quote;90&quote; attenuation&equal;&quote;0&quote; usage&equal;&quote;ABS&quote; /></ReadingLabel></MeasurementReading></Well><CustomData id&equal;&quote;12&quote; /></MeasurementKinetic></MeasurementAbsorbance></PlateRange></CyclePlate></MeasurementManualCycle><MeasurementInfo id&equal;&quote;0&quote; description&equal;&quote;&quote;><ScriptTemplateSettings id&equal;&quote;0&quote;><ScriptTemplateGeneralSettings id&equal;&quote;0&quote; Title&equal;&quote;&quote; Group&equal;&quote;&quote; Info&equal;&quote;&quote; Image&equal;&quote;&quote; /><ScriptTemplateDescriptionSettings id&equal;&quote;0&quote; Internal&equal;&quote;&quote; External&equal;&quote;&quote; IsExternal&equal;&quote;False&quote; /></ScriptTemplateSettings></MeasurementInfo></TecanMeasurement></TecanFile>","0","");
			val programData = FileUtils.readFileToString(cmd.programFile).replace("\r", "").replace("\n", "").replace("=", "&equal;").replace("\"", "&quote;")
			// Token
			val token = L0C_Facts(deviceName, deviceName+"_Measure", cmd.outputFilename + "|" + programData)
			// Return
			val item = TranslationItem(token, siteToModel_l.flatten)
			List(item)
		}
	}

}