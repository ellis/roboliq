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
	val userInitialConditionsBSSE = """  (device-can-site r1_pipetter1 r1_bench_017x1)
  (device-can-site r1_pipetter1 r1_bench_017x2)
  (device-can-site r1_pipetter1 r1_bench_017x3)
  (device-can-site r1_pipetter1 r1_bench_017x4)
"""
	val userInitialConditionsWIS = """  (device-can-site r1_pipetter1 r1_bench_035x1)
  (device-can-site r1_pipetter1 r1_bench_035x2)
  (device-can-site r1_pipetter1 r1_bench_035x3)
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
			{ "name": "plate1(A01 d D01)", "contents": "water@200ul" }
		],
		"protocol": [
			{ "command": "distribute", "source": "plate1(A01)", "destination": "plate2(A01)", "volume": "50ul" },
			{ "command": "distribute", "source": "plate1(A01)", "destination": "plate2(A01 d B01)", "volume": "50ul" },
			{ "command": "distribute", "source": "plate1(C01 d D01)", "destination": "plate2(C01 d D01)", "volume": "50ul" }
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
(!pipetter-run r1 r1_pipetter1 spec0009)
		"""
	)
	
	val ph = (
		"""{
		"substances": [
			{ "name": "water", "kind": "Liquid", "tipCleanPolicy": "ThoroughNone" },
			{ "name": "buffer10x", "kind": "Liquid", "tipCleanPolicy": "Thorough" },
			{ "name": "dntp", "kind": "Liquid", "tipCleanPolicy": "Decontaminate" },
			{ "name": "taqdiluted", "kind": "Liquid", "tipCleanPolicy": "Decontaminate" },
			{ "name": "template", "kind": "Dna", "tipCleanPolicy": "Decontaminate" },
			{ "name": "primer1", "kind": "Dna", "tipCleanPolicy": "Decontaminate" },
			{ "name": "primer2", "kind": "Dna", "tipCleanPolicy": "Decontaminate" }
		],
		"plates": [
			{ "name": "plate1", "model": "Thermocycler Plate", "location": "offsite"},
			{ "name": "plate2", "model": "Thermocycler Plate", "location": "offsite"}
		],
		"wellContents": [
			{ "name": "plate1(A01 d D01)", "contents": "water@200ul" },
			{ "name": "plate1(A02)", "contents": "buffer10x@200ul" },
			{ "name": "plate1(B02)", "contents": "dntp@200ul" },
			{ "name": "plate1(C02)", "contents": "template@200ul" },
			{ "name": "plate1(D02)", "contents": "primer1@200ul" },
			{ "name": "plate1(E02)", "contents": "primer2@200ul" },
			{ "name": "plate1(F02)", "contents": "taqdiluted@200ul" }
		],
		"protocol": [
			{ "command": "pipette", "steps": [
				{ "command": "distribute", "source": "plate1(A01 d D01)", "destination": "plate2(A01 d D01)", "volume": "18ul", "pipettePolicy": "Water_C_1000", "cleanBefore": "Thorough" },
				{ "command": "distribute", "source": "plate1(A02)", "destination": "plate2(A01 d D01)", "volume": "3ul", "pipettePolicy": "Water_C_1000" },
				{ "command": "distribute", "source": "plate1(B02)", "destination": "plate2(A01 d D01)", "volume": "3ul", "pipettePolicy": "Water_C_1000", "cleanBefore": "Decontaminate" },
				{ "command": "distribute", "source": "plate1(C02)", "destination": "plate2(A01 d D01)", "volume": "1.5ul", "pipettePolicy": "Water_C_50", "cleanBefore": "Decontaminate" },
				{ "command": "distribute", "source": "plate1(D02)", "destination": "plate2(A01 d D01)", "volume": "1.5ul", "pipettePolicy": "Water_C_50", "cleanBefore": "Decontaminate" },
				{ "command": "distribute", "source": "plate1(E02)", "destination": "plate2(A01 d D01)", "volume": "1.5ul", "pipettePolicy": "Water_C_50", "cleanBefore": "Decontaminate" },
				{ "command": "distribute", "source": "plate1(F02)", "destination": "plate2(A01 d D01)", "volume": "1.5ul", "pipettePolicy": "Water_C_50", "cleanBefore": "Decontaminate" }
				]
			}
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
		"""
	)
	
	val pi = (
		"""{
		"substances": [
			{ "name": "water", "kind": "Liquid", "tipCleanPolicy": "ThoroughNone" },
			{ "name": "buffer10x", "kind": "Liquid", "tipCleanPolicy": "Thorough" },
			{ "name": "dntp", "kind": "Liquid", "tipCleanPolicy": "Decontaminate" },
			{ "name": "taqdiluted", "kind": "Liquid", "tipCleanPolicy": "Decontaminate" },
			{ "name": "template", "kind": "Dna", "tipCleanPolicy": "Decontaminate" },
			{ "name": "primer1", "kind": "Dna", "tipCleanPolicy": "Decontaminate" },
			{ "name": "primer2", "kind": "Dna", "tipCleanPolicy": "Decontaminate" }
		],
		"plates": [
			{ "name": "plate1", "model": "Thermocycler Plate", "location": "offsite"},
			{ "name": "plate2", "model": "Thermocycler Plate", "location": "offsite"}
		],
		"wellContents": [
			{ "name": "plate1(A01 d D01)", "contents": "water@200ul" },
			{ "name": "plate1(A02)", "contents": "buffer10x@200ul" },
			{ "name": "plate1(B02)", "contents": "dntp@200ul" },
			{ "name": "plate1(C02)", "contents": "template@200ul" },
			{ "name": "plate1(D02)", "contents": "primer1@200ul" },
			{ "name": "plate1(E02)", "contents": "primer2@200ul" },
			{ "name": "plate1(F02)", "contents": "taqdiluted@200ul" }
		],
		"protocol": [
			{ "command": "pipette", "steps": [
				{ "command": "distribute", "source": "plate1(A01 d D01)", "destination": "plate2(A01 d D01)", "volume": "18ul", "pipettePolicy": "Roboliq_Water_Wet_1000", "cleanBefore": "Thorough" },
				{ "command": "distribute", "source": "plate1(A02)", "destination": "plate2(A01 d D01)", "volume": "3ul", "pipettePolicy": "Roboliq_Water_Wet_1000" },
				{ "command": "distribute", "source": "plate1(B02)", "destination": "plate2(A01 d D01)", "volume": "3ul", "pipettePolicy": "Roboliq_Water_Wet_1000", "cleanBefore": "Decontaminate", "cleanAfter": "Decontaminate" },
				{ "command": "distribute", "source": "plate1(C02)", "destination": "plate2(A01 d D01)", "volume": "1.5ul", "pipettePolicy": "Roboliq_Water_Wet_0050", "cleanBefore": "Decontaminate" },
				{ "command": "distribute", "source": "plate1(D02)", "destination": "plate2(A01 d D01)", "volume": "1.5ul", "pipettePolicy": "Roboliq_Water_Wet_0050", "cleanBefore": "Decontaminate" },
				{ "command": "distribute", "source": "plate1(E02)", "destination": "plate2(A01 d D01)", "volume": "1.5ul", "pipettePolicy": "Roboliq_Water_Wet_0050", "cleanBefore": "Decontaminate" },
				{ "command": "distribute", "source": "plate1(F02)", "destination": "plate2(A01 d D01)", "volume": "1.5ul", "pipettePolicy": "Roboliq_Glycerol_Wet_0050", "cleanBefore": "Decontaminate", "cleanAfter": "Decontaminate" }
				]
			},
			{ "command": "thermocycle", "object": "plate1", "spec": "thermocyclerSpec1" }
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
	
	val wa = (
		"""{
		"substances": [
			{ "name": "kod", "kind": "Liquid", "tipCleanPolicy": "ThoroughNone" },
			{ "name": "template", "kind": "Dna", "tipCleanPolicy": "Decontaminate" },
			{ "name": "primer1", "kind": "Dna", "tipCleanPolicy": "Decontaminate" },
			{ "name": "primer2", "kind": "Dna", "tipCleanPolicy": "Decontaminate" }
		],
		"plates": [
			{ "name": "pcrPlate1", "model": "Thermocycler Plate", "location": "offsite" },
			{ "name": "reagentPlate1", "model": "Thermocycler Plate", "location": "offsite" }
		],
		"wellContents": [
			{ "name": "reagentPlate1(A01 d D01)", "contents": "kod@200ul" },
			{ "name": "reagentPlate1(D02)", "contents": "primer1@200ul" },
			{ "name": "reagentPlate1(E02)", "contents": "primer2@200ul" },
			{ "name": "reagentPlate1(C02)", "contents": "template@200ul" }
		],
		"protocol": [
			{ "command": "pipette", "steps": [
				{ "command": "distribute", "source": "reagentPlate1(A01 d D01)", "destination": "pcrPlate1(A01 d D01)", "volume": "8ul", "pipettePolicy": "BOT_BOT_LIQUID", "cleanBefore": "Decontaminate" },
				{ "command": "distribute", "source": "reagentPlate1(D02)", "destination": "pcrPlate1(A01 d D01)", "volume": "13ul", "pipettePolicy": "BOT_BOT_LIQUID", "cleanBefore": "Decontaminate" },
				{ "command": "distribute", "source": "reagentPlate1(E02)", "destination": "pcrPlate1(A01 d D01)", "volume": "13ul", "pipettePolicy": "BOT_BOT_LIQUID", "cleanBefore": "Decontaminate" },
				{ "command": "distribute", "source": "reagentPlate1(C02)", "destination": "pcrPlate1(A01 d D01)", "volume": "7ul", "pipettePolicy": "BOT_BOT_LIQUID", "cleanBefore": "Decontaminate" }
				]
			}
		]
		}""",
		"""(!agent-activate user)
(!transporter-run user userarm reagentplate1 m001 offsite r1_bench_035x1 userarmspec)
(!transporter-run user userarm pcrplate1 m001 offsite r1_bench_035x2 userarmspec)
(!agent-deactivate user)
(!agent-activate r1)
(!pipetter-run r1 r1_pipetter1 spec0003)
		"""
	)
	
	def run(protocolName: String, input: String, output: String) {
		for {
			carrierData <- roboliq.evoware.parser.EvowareCarrierData.loadFile("./testdata/bsse-robot1/config/carrier.cfg")
			tableData <- roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, "./testdata/bsse-robot1/config/table-01.esc")
		} {
			protocol.loadConfig_Weizmann()
			protocol.loadEvoware("r1", carrierData, tableData)
			protocol.loadJson(input.asJson.asJsObject)
			
			protocol.saveProblem(s"tasks/autogen/$protocolName.lisp", userInitialConditionsBSSE)
			
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
	
	def runWeizmann(protocolName: String, input: String, taskOutput: String) {
		val x = for {
			carrierData <- roboliq.evoware.parser.EvowareCarrierData.loadFile("./testdata/weizmann-sealy/config/carrier.cfg")
			tableData <- roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, "./testdata/weizmann-sealy/config/table-01.esc")
			
			_ = protocol.loadConfig_Weizmann()
			_ = protocol.loadEvoware("r1", carrierData, tableData)
			_ = protocol.loadJson(input.asJson.asJsObject)
			
			_ = protocol.saveProblem(s"tasks/wisauto/$protocolName.lisp", userInitialConditionsWIS)
			
			configData = EvowareConfigData(Map("G009S1" -> "pipette2hi"))
			config = new EvowareConfig(carrierData, tableData, configData)
			scriptBuilder = new EvowareClientScriptBuilder(config, s"tasks/wisauto/$protocolName")
			agentToBuilder_m = Map[String, ClientScriptBuilder](
				"user" -> scriptBuilder,
				"r1" -> scriptBuilder
			)
			result <- JshopTranslator.translate(protocol, taskOutput, agentToBuilder_m)
		} yield {
			for (script <- scriptBuilder.script_l) {
				scriptBuilder.saveWithHeader(script, script.filename)
			}
		}

		val error_l = x.getErrors
		val warning_l = x.getWarnings
		if (!error_l.isEmpty || !warning_l.isEmpty) {
			println("Warnings and Errors:")
			error_l.foreach(println)
			warning_l.foreach(println)
			println()
		}
	}
	
	//run("pd", pd._1, pd._2)
	//run("pf", pf._1, pf._2)
	//run("pg", pg._1, pg._2)
	//run("ph", ph._1, ph._2)
	//run("pi", pi._1, pi._2)
	
	runWeizmann("pa", wa._1, wa._2)
}