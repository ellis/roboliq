package roboliq.input

import org.scalatest.FunSpec
import roboliq.utils.JsonUtils
import roboliq.utils.MiscUtils
import spray.json.JsObject
import spray.json.JsArray

class TestInstructionExtractorSpec extends FunSpec {
	import roboliq.input.ResultCWrapper._

	val input1a = JsonUtils.yamlToJson("""
    objects:
      ourlab:
        type: Namespace
        mario:
          type: Namespace
          evoware: { type: EvowareRobot }
          arm1: { type: Transporter, evowareRoma: 0 }
          P2: { type: Site, evowareCarrier: "MP 2Pos H+P Shake", evowareGrid: 10, evowareSite: 2 }
          P3: { type: Site, evowareCarrier: "MP 2Pos H+P Shake", evowareGrid: 10, evowareSite: 4 }
        model1: { type: PlateModel, evowareName: Ellis Nunc F96 MicroWell }
""").asJsObject

	val input1b = JsonUtils.yamlToJson("""
    objects:
      plate1: { type: Plate, model: ourlab.model1 }
    steps:
      1: {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P3}
      2: {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P2}
""").asJsObject

	val input1c = JsonUtils.yamlToJson("""
    objects:
      plate1: { location: ourlab.mario.P2 }
""").asJsObject

	val input1 = JsonUtils.merge(List(input1a, input1b, input1c)).run().value.asJsObject
	
	describe("Test some instruction extractor") {
		it("hmm") {
			val step_m = input1.fields("steps").asJsObject
			val key_l = step_m.fields.keys.toList.sortWith((s1, s2) => MiscUtils.compareNatural(s1, s2) < 0)
			val step_l0 = key_l.map(step_m.fields)
			val output = JsObject(input1.fields  + ("steps" -> JsArray(step_l0)))
			println(output.prettyPrint)
		}
	}
}