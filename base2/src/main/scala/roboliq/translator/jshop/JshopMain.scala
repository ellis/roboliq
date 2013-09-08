package roboliq.translator.jshop

import spray.json._
import roboliq.core._
import roboliq.input.Protocol
import roboliq.evoware.translator.EvowareConfigData
import roboliq.evoware.translator.EvowareConfig
import roboliq.evoware.translator.EvowareClientScriptBuilder
import roboliq.entities.ClientScriptBuilder

object JshopMain extends App {
	val protocol = new Protocol

	val userInitialConditions = """  (is-pipetter r1_pipetter1) 
  (agent-has-device r1 r1_pipetter1)
  (device-can-site r1_pipetter1 r1_bench_017x1)
  (device-can-site r1_pipetter1 r1_bench_017x2)
  (device-can-site r1_pipetter1 r1_bench_017x3)
  (device-can-site r1_pipetter1 r1_bench_017x4)
"""
	
	val pd = (
		"""{
		"plates": [
			{ "name": "plate1", "model": "Thermocycler Plate", "location": "offsite"}
		],
		"protocol": [
			{ "command": "log", "text": "do the right thing" },
			{ "command": "prompt", "text": "Please do the right thing, then press ENTER." },
			{ "command": "move", "labware": "plate1", "destination": "r1_bench_017x1" }
		]
		}""",
		"""(!log r1 text0002)
		(!prompt r1 text0004)
		(!agent-activate user)
		(!transporter-run user userarm plate1 m002 offsite r1_hotel_245x1 userarmspec)
		(!agent-deactivate user)
		(!agent-activate r1)
		(!transporter-run r1 r1_transporter2 plate1 m002 r1_hotel_245x1 r1_bench_017x1 r1_transporterspec0)
		"""
	)
	
	val pe = (
		"""{
		"plates": [
			{ "name": "plate1", "model": "Thermocycler Plate", "location": "offsite"}
		],
		"protocol": [
			{ "command": "distribute", "source": "plate1(A01)", "destination": "plate1(B01)", "volume": "50ul" }
		]
		}""",
		"""(!agent-activate user)
(!transporter-run user userarm plate1 m002 offsite r1_hotel_245x1 userarmspec)
(!agent-deactivate user)
(!agent-activate r1)
(!transporter-run r1 r1_transporter2 plate1 m002 r1_hotel_245x1 r1_bench_017x1 r1_transporterspec0)
(!pipetter-run r1 r1_pipetter1 spec0003)
		"""
	)
	
	def run(protocolName: String, input: String, output: String) {
		for {
			carrierData <- roboliq.evoware.parser.EvowareCarrierData.loadFile("./testdata/bsse-robot1/config/carrier.cfg")
			tableData <- roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, "./testdata/bsse-robot1/config/table-01.esc")
		} {
			protocol.loadConfig()
			protocol.loadEvoware("r1", carrierData, tableData)
			protocol.loadJson(input.asJson.asJsObject)
			
			protocol.saveProblem(protocolName, userInitialConditions)
			
			val taskOutput = output

			val configData = EvowareConfigData(Map("G009S1" -> "pipette2hi"))
			val config = new EvowareConfig(carrierData, tableData, configData)
			val scriptBuilder = new EvowareClientScriptBuilder(config, s"tasks/autogen/$protocolName")
			val agentToBuilder_m = Map[String, ClientScriptBuilder](
				"user" -> scriptBuilder,
				"r1" -> scriptBuilder
			)
			val result_? = JshopTranslator2.translate(protocol, taskOutput, agentToBuilder_m)
			println("Warnings and Errors:")
			result_?.getErrors.foreach(println)
			result_?.getWarnings.foreach(println)
			println()
	
			if (result_?.isSuccess) {
				for (script <- scriptBuilder.script_l) {
					scriptBuilder.saveWithHeader(script, script.filename)
				}
				//translator.saveWithHeader(script, s"tasks/autogen/$protocolName.esc")
				//script.cmds.foreach(println)
			}
		}
	}
	
	//run("pd", pd._1, pd._2)
	run("pe", pe._1, pe._2)
}