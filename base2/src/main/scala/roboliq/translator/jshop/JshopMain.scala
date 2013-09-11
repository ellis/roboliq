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

	// HACK: this belongs in a general config file
	val userInitialConditions = """  (device-can-site r1_pipetter1 r1_bench_017x1)
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
		"substances": [
			{ "name": "water", "kind": "Liquid", "tipCleanPolicy": "ThoroughNone" }
		],
		"plates": [
			{ "name": "plate1", "model": "Thermocycler Plate", "location": "offsite"}
		],
		"wellContents": [
			{ "name": "plate1(A01)", "contents": "water@100ul" }
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
	
	val pf = (
		"""{
		"substances": [
			{ "name": "water", "kind": "Liquid", "tipCleanPolicy": "ThoroughNone" }
		],
		"plates": [
			{ "name": "plate1", "model": "Thermocycler Plate", "location": "offsite"}
		],
		"wellContents": [
			{ "name": "plate1(A01)", "contents": "water@100ul" }
		],
		"protocol": [
			{ "command": "distribute", "source": "plate1(A01)", "destination": "plate1(B01)", "volume": "50ul" },
			{ "command": "thermocycle", "object": "plate1", "spec": "thermocyclerSpec1" }
		]
		}""",
		"""(!agent-activate user)
(!transporter-run user userarm plate1 m002 offsite r1_hotel_245x1 userarmspec)
(!agent-deactivate user)
(!agent-activate r1)
(!transporter-run r1 r1_transporter2 plate1 m002 r1_hotel_245x1 r1_bench_017x1 r1_transporterspec0)
(!pipetter-run r1 r1_pipetter1 spec0003)
(!transporter-run r1 r1_transporter2 plate1 m002 r1_bench_017x1 r1_device_236x1 r1_transporterspec0)
(!sealer-run r1 r1_sealer sealerspec1 plate1 r1_device_236x1)
(!thermocycler-open r1 r1_thermocycler1)
(!transporter-run r1 r1_transporter2 plate1 m002 r1_device_236x1 r1_device_234x1 r1_transporterspec0)
(!thermocycler-close r1 r1_thermocycler1)
(!thermocycler-run r1 r1_thermocycler1 thermocyclerspec1)
(!thermocycler-open r1 r1_thermocycler1)
(!thermocycler-close r1 r1_thermocycler1)
		"""
	)
	
	val pg = (
		"""{
		"substances": [
			{ "name": "water", "kind": "Liquid", "tipCleanPolicy": "ThoroughNone" }
		],
		"plates": [
			{ "name": "plate1", "model": "Thermocycler Plate", "location": "offsite"},
			{ "name": "plate2", "model": "Thermocycler Plate", "location": "offsite"}
		],
		"wellContents": [
			{ "name": "plate1(A01)", "contents": "water@100ul" }
		],
		"protocol": [
			{ "command": "distribute", "source": "plate1(A01)", "destination": "plate2(A01)", "volume": "50ul" },
			{ "command": "distribute", "source": "plate1(A01)", "destination": "plate2(A01 d B01)", "volume": "50ul" }
		]
		}""",
		"""(!agent-activate user)
(!transporter-run user userarm plate1 m002 offsite r1_hotel_245x1 userarmspec)
(!agent-deactivate user)
(!agent-activate r1)
(!transporter-run r1 r1_transporter2 plate1 m002 r1_hotel_245x1 r1_bench_017x1 r1_transporterspec0)
(!agent-deactivate r1)
(!agent-activate user)
(!transporter-run user userarm plate2 m002 offsite r1_hotel_245x1 userarmspec)
(!agent-deactivate user)
(!agent-activate r1)
(!transporter-run r1 r1_transporter2 plate2 m002 r1_hotel_245x1 r1_bench_017x3 r1_transporterspec0)
(!pipetter-run r1 r1_pipetter1 spec0003)
(!pipetter-run r1 r1_pipetter1 spec0006)
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
			val result_? = JshopTranslator.translate(protocol, taskOutput, agentToBuilder_m)
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
	//run("pf", pf._1, pf._2)
	run("pg", pg._1, pg._2)
}