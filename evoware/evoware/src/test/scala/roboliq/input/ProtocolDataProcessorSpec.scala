package roboliq.input

import org.scalatest.FunSpec
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsString
import spray.json.JsValue
import spray.json._
import java.io.File
import roboliq.utils.JsonUtils
import roboliq.ai.strips
import roboliq.ai.plan.Unique

class ProtocolDataProcessorSpec extends FunSpec {
	import ContextValueWrapper._
	import ResultCWrapper._

	val data0 = ResultEData(EvaluatorState(searchPath_l = List(new File("testfiles"), new File("base/testfiles"))))
	val evaluator = new Evaluator()
	
	def getProtocolData(json: String): ProtocolData = {
		val jsProtocol = json.parseJson
		val rjsProtocol = RjsValue.fromJson(jsProtocol).run().value
		val protocol = RjsConverterC.fromRjs[Protocol](rjsProtocol).run().value
		val protocolData = ProtocolData.fromProtocol(protocol).run().value
		protocolData
	}

	describe("ProtocolDataProcessorSpec") {
		it("variables") {
			val protocolData1a = getProtocolData("""{
				"variables": {
					"totalVolume": { "type": "Integer", "description": "Please enter the total volume" }
				}
			}""")
			val protocolData1b = ProtocolDataProcessor.processVariables(protocolData1a).run().value
			assert(protocolData1b.settings == Map("variables.totalVolume.value" -> ProtocolDataSetting(None, List("Missing variable value"), Nil)))

			val protocolData2a = getProtocolData("""{
				"variables": {
					"totalVolume": { "type": "Integer", "description": "Please enter the total volume", "value": 20 }
				}
			}""")
			val protocolData2b = ProtocolDataProcessor.processVariables(protocolData2a).run().value
			assert(protocolData2b.settings == Map())
		}
		
		it("materials (plates)") {
			val plateModelName_l = List("96 well square", "384 well round")

			val protocolData1a = getProtocolData("""{
				"materials": {
					"plate1": { "type": "Plate" }
				}
			}""")
			val protocolData1b = ProtocolDataProcessor.processMaterials(protocolData1a, plateModelName_l).run().value
			assert(protocolData1b.settings == Map("materials.plate1.model" -> ProtocolDataSetting(None, List("Missing plate model"), plateModelName_l.toList.map(RjsString))))

			val protocolData2a = getProtocolData("""{
				"materials": {
					"plate1": { "type": "Plate", "model": "96 well square" }
				}
			}""")
			val protocolData2b = ProtocolDataProcessor.processMaterials(protocolData2a, plateModelName_l).run().value
			assert(protocolData2b.settings == Map())
		}
		
		it("tasks") {
			val method_l = List("mixByShaker")
			val taskToMethods_m: Map[String, List[String]] = Map("mix" -> method_l)

			val protocolData1a = getProtocolData("""{
				"steps": {
					"1": { "command": "mix" }
				}
			}""")
			val protocolData1b = ProtocolDataProcessor.processTasks(protocolData1a, taskToMethods_m).run().value
			assert(protocolData1b.settings == Map("steps.1.method" -> ProtocolDataSetting(None, Nil, method_l.map(RjsString))))

			val protocolData2a = getProtocolData("""{
				"steps": {
					"1": { "command": "mix", "method": "mixByShaker" }
				}
			}""")
			val protocolData2b = ProtocolDataProcessor.processTasks(protocolData2a, taskToMethods_m).run().value
			assert(protocolData2b.settings == Map())
			
			// TODO: make sure methods are placed within the tasks now
		}
		
		it("methods") {
			val methodExpansions_m = Map[String, String](
				"mixByShaker" -> """{
					"1": {
						"command": "shake"
					}
				}"""
			)
		}
	}
}