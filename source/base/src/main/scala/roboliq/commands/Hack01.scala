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
import roboliq.entities.Reader
import java.io.File
import spray.json.JsNumber
import org.apache.commons.io.FileUtils
import roboliq.entities.PipetteSources
import roboliq.entities.PipetteDestinations
import roboliq.entities.PipetteAmount_Volume
import roboliq.entities.LiquidVolume
import roboliq.entities.CleanIntensity
import roboliq.entities.RowCol


case class Hack01ActionParams(
	source: PipetteSources,
	destination: PipetteDestinations
)

class Hack01ActionHandler extends ActionHandler {

	private val mdfxTemplate =
"""<?xml version="1.0" encoding="UTF-8"?>
<TecanFile xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="tecan.at.schema.documents Main.xsd" fileformat="Tecan.At.Measurement" fileversion="2.0" xmlns="tecan.at.schema.documents">
    <FileInfo type="" instrument="infinite 200Pro" version="" createdFrom="localadmin" createdAt="2014-10-30T15:31:20.4665662Z" createdWith="Tecan.At.XFluor.ReaderEditor.XFluorReaderEditor" description="" />
    <TecanMeasurement id="1" class="">
        <MeasurementManualCycle id="2" number="1" type="Standard">
            <CyclePlate id="3" file="PE384fw_OptiPlate" plateWithCover="False">
                <PlateRange id="4" range="{{WELLS}}" auto="false">
                    <MeasurementFluoInt readingMode="Top" id="5" mode="Normal" type="" name="FluoInt" longname="" description="">
                        <Well id="6" auto="false">
                            <Injection id="7" command="" channels="1" volumes="86" speeds="200" refillspeeds="100" refillvolumes="500" injectionmode="StandardInjection" injectiontype="Refill" maxPacketSize="1" readtimelikedispensetime="False" />
                            <MeasurementWellKinetic id="8" loops="1000" timeSpan="PT0.3S" maxDeviation="P10675199DT2H48M5.4775807S" duration="PT1M" useDuration="false">
                                <Condition id="9" expression="CYCLE.CURRENT.8==10">
                                    <Injection id="10" command="" channels="1" volumes="86" speeds="200" refillspeeds="100" refillvolumes="500" injectionmode="StandardInjection" injectiontype="Inject" maxPacketSize="1" readtimelikedispensetime="False" />
                                </Condition>
                                <MeasurementReading id="11" name="" refID="8" refTimeMaxDeviation="PT0.3S" refName="KINETIC.RUN.CYCLE" beamDiameter="3000" beamGridType="Single" beamGridSize="1" beamEdgeDistance="auto">
                                    <ReadingLabel id="12" name="Label1" scanType="ScanFixed" refID="0">
                                        <ReadingSettings number="2" rate="25000" />
                                        <ReadingGain type="" gain="60" optimalGainPercentage="0" automaticGain="False" mode="Manual" />
                                        <ReadingTime integrationTime="20" lagTime="0" readDelay="0" flash="0" dark="0" excitationTime="0" />
                                        <ReadingFilter id="13" type="Ex" wavelength="4750" bandwidth="90" attenuation="0" usage="FI" />
                                        <ReadingFilter id="14" type="Em" wavelength="5100" bandwidth="200" attenuation="0" usage="FI" />
                                        <ReadingZPosition mode="Manual" zPosition="20000" />
                                    </ReadingLabel>
                                </MeasurementReading>
                            </MeasurementWellKinetic>
                        </Well>
                    </MeasurementFluoInt>
                </PlateRange>
            </CyclePlate>
        </MeasurementManualCycle>
        <MeasurementInfo id="0" description="">
            <ScriptTemplateSettings id="0">
                <ScriptTemplateGeneralSettings id="0" Title="" Group="" Info="" Image="" />
                <ScriptTemplateDescriptionSettings id="0" Internal="" External="" IsExternal="False" />
            </ScriptTemplateSettings>
        </MeasurementInfo>
    </TecanMeasurement>
</TecanFile>"""
	
	def getActionName = "hack01"

	def getActionParamNames = List("source", "destination")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[Hack01ActionParams](paramToJsval_l, eb, state0)
		} yield {
			val suffix = id.mkString("__", "_", "")
			val agentName = "mario"

			def getActionParamNames = List("agent", "device", "source", "destination", "amount", "clean", "cleanBegin", "cleanBetween", "cleanBetweenSameSource", "cleanEnd", "pipettePolicy", "tipModel", "tip")

			val transport1Params = List(
				"device" -> JsString("mario_transporter2"),
				"object" -> JsString("renaturationPlate"),
				"site" -> JsString("REGRIP")
			)
			val measureParams = List(
				"object" -> JsString("renaturationPlate")
			)
			val transport2Params = List(
				"device" -> JsString("mario_transporter2"),
				"object" -> JsString("renaturationPlate"),
				"site" -> JsString("P2")
			)

			val plate1Name = "mixPlate"
			val plateModel1Name = "plateModel_96_pcr"
			val plate2Name = "renaturationPlate"
			val plateModel2Name = "plateModel_384_square"
			val siteModelDOWNHOLDERName = "$siteModelDOWNHOLDER"+suffix
			val siteModelP2Name = "$siteModelP2"+suffix
			val siteModelREGRIPName = "$siteModelREGRIP"+suffix
			val siteModelREADERName = "$siteModelREADER"+suffix
			
			var index = 0
			def getIndex: List[Int] = {
				index += 1
				id ++ List(index)
			}

			var dstAll_l = params.destination.l
			// Repeat for each source well:
			params.source.sources.flatMap(src => {
				val sourceName = src.l.head.toString
				val dst_l = dstAll_l.take(3)
				dstAll_l = dstAll_l.drop(3)
				val destinationName = dst_l.map(_.toString).mkString("+")
				val destinationNameMdfx = dst_l.map(x => rowcolToStringMdfx(x.rowcol)).mkString("|")
				val oi1 = {
					/*
					val src = params.source.sources.head
					val dst_l = params.destination.l.take(3)
					val distributeParams = DistributeActionParams(
						agent_? = Some(agentName),
						device_? = None,
						source = src,
						destination = PipetteDestinations(dst_l),
						amount = List(PipetteAmount_Volume(LiquidVolume.ul(4.5))),
						clean_? = None,
						cleanBegin_? = None,
						cleanBetween_? = Some(CleanIntensity.None),
						cleanBetweenSameSource_? = None,
						cleanEnd_? = None,
						pipettePolicy_? = Some("Roboliq_Water_Dry_0050"),
						tipModel_? = None,
						tip_? = Some(5)
					)
					*/
					val distributeBindings = Map[String, String](
						"?agent" -> agentName,
						"?device" -> "mario__pipetter1",
						"?labware1" -> plate1Name,
						"?model1" -> plateModel1Name,
						"?site1" -> "DOWNHOLDER",
						"?siteModel1" -> siteModelDOWNHOLDERName,
						"?labware2" -> plate2Name,
						"?model2" -> plateModel2Name,
						"?site2" -> "P2",
						"?siteModel2" -> siteModelP2Name
					)
					val distributeParam_m = Map[String, JsValue](
						"source" -> JsString(sourceName),
						"destination" -> JsString(destinationName),
						"amount" -> JsString("4.5ul"),
						"cleanBetween" -> JsString("none"),
						"tip" -> JsNumber(5),
						"pipettePolicy" -> JsString("Roboliq_Water_Dry_0050")
					)
					
					OperatorInfo(getIndex, Nil, Nil, s"distribute2", distributeBindings, distributeParam_m)
				}
				
				val oi2 = {
					val binding_m = Map[String, String](
						"?labware" -> plate2Name,
						"?model" -> plateModel2Name,
						"?site1" -> "P2",
						"?site2" -> "REGRIP",
						"?siteModel2" -> siteModelREGRIPName
					)
					val param_m = Map[String, JsValue](
						"agent" -> JsString(agentName),
						"device" -> JsString("mario__transporter2")
					)
					OperatorInfo(getIndex, Nil, Nil, "transportLabware", binding_m, param_m)
				}
				
				val oi3_l = {
					val labwareName = plate2Name
					val modelName = plateModel2Name
					val site1Name = "P2"
					val siteModel1Name = siteModelP2Name
					val deviceName = "mario__Infinite_M200"
					val siteName = "READER"
						
					// Bindings for transfer to sealer
					val bindingOpenClose_m = Map[String, String](
						"?agent" -> agentName,
						"?device" -> deviceName,
						"?site" -> siteName
					)
					// Bindings for transfer to sealer
					val bindingTransportBefore2_m = Map(
						"?labware" -> labwareName,
						"?model" -> modelName,
						"?site1" -> "REGRIP",
						"?siteModel1" -> siteModelREGRIPName,
						"?site2" -> siteName,
						"?siteModel2" -> siteModelREADERName
					)
					// Bindings for transfer to sealer
					val bindingTransportAfter1_m = Map(
						"?labware" -> labwareName,
						"?model" -> modelName,
						"?site1" -> siteName,
						"?siteModel1" -> siteModelREADERName,
						"?site2" -> "REGRIP",
						"?siteModel2" -> siteModelREGRIPName
					)
					// Binding for the actual measurement
					val bindingMeasure_m = Map(
						"?agent" -> agentName,
						"?device" -> deviceName,
						"?labware" -> labwareName,
						"?model" -> modelName,
						"?site" -> siteName
					)
					val paramMeasure_m = Map[String, JsValue](
						"programData" -> JsString(mdfxTemplate.replace("{{WELLS}}", destinationNameMdfx)),
						"outputFile" -> JsString("""C:\Users\localadmin\Desktop\Ellis\tania10_renaturation--<YYYYMMDD_HHmmss>.xls"""),
						"object" -> JsString(plate2Name)
					)
					
					List(
						OperatorInfo(getIndex, Nil, Nil, "openDeviceSite", bindingOpenClose_m, Map()),
						OperatorInfo(getIndex, Nil, Nil, "transportLabware", bindingTransportBefore2_m, Map()),
						OperatorInfo(getIndex, Nil, Nil, "closeDeviceSite", bindingOpenClose_m, Map()),
						OperatorInfo(getIndex, Nil, Nil, "measureAbsorbance", bindingMeasure_m, paramMeasure_m),
						OperatorInfo(getIndex, Nil, Nil, "openDeviceSite", bindingOpenClose_m, Map()),
						OperatorInfo(getIndex, Nil, Nil, "transportLabware", bindingTransportAfter1_m, Map()),
						OperatorInfo(getIndex, Nil, Nil, "closeDeviceSite", bindingOpenClose_m, Map())
					)
				}
				
				val oi4 = {
					val binding_m = Map[String, String](
						"?labware" -> plate2Name,
						"?model" -> plateModel2Name,
						"?site1" -> "REGRIP",
						"?siteModel1" -> siteModelREGRIPName,
						"?site2" -> "P2",
						"?siteModel2" -> siteModelP2Name
					)
					val param_m = Map[String, JsValue](
						"agent" -> JsString(agentName),
						"device" -> JsString("mario__transporter2")
					)
					OperatorInfo(getIndex, Nil, Nil, "transportLabware", binding_m, param_m)
				}
				
				oi1 :: oi2 :: oi3_l ++ List(oi4)
			})
		}
	}

	private def rowcolToStringMdfx(rowcol: RowCol): String = {
		val s = (rowcol.row + 'A').asInstanceOf[Char].toString + (rowcol.col + 1)
		s+":"+s
	}

}
