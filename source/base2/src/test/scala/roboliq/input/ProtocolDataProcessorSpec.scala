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

	describe("ProtocolDataProcessorSpec") {
		it("missing variable") {
			val jsProtocol = """{
				"variables": {
					"totalVolume": { "type": "Integer", "description": "Please enter the total volume" }
				}
			}""".parseJson
			val rjsProtocol = RjsValue.fromJson(jsProtocol).run().value
			val protocol = RjsConverterC.fromRjs[Protocol](rjsProtocol).run().value
			val protocolData0 = ProtocolData.fromProtocol(protocol).run().value
			val protocolData1 = ProtocolDataProcessor.processVariables(protocolData0).run().value
			assert(protocolData1.settings == Map("variables.totalVolume.value" -> ProtocolDataSetting(None, List("You must set the value"), Nil)))
		}
	}
}