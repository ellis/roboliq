package roboliq.input

import org.scalatest.FunSpec
import roboliq.entities.EntityBase
import roboliq.core.RsSuccess
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsString
import spray.json.JsValue
import java.io.File
import roboliq.utils.JsonUtils
import aiplan.strips2.Strips

class Protocol2Spec extends FunSpec {
	val protocol = new Protocol2
	val eb = new EntityBase
	val data0 = ContextEData(EvaluatorState(eb, searchPath_l = List(new File("testfiles"))))
	val evaluator = new Evaluator();
	
	describe("Protocol2Spec") {
		it("processData") {
			val yaml = """
object:
  plate1:
    type: plate
    model: plateModel_384_square
    location: P3
  "plate1(A01)":
    type: well
    content: water
  water:
    type: substance
  dye:
    type: substance
  dyeLight:
    type: source
    source: plate1(A01)
    substance:
      dye:
        amount: 1/10
      water:
        amount: 9/10
cmd:
- index: [1]
  command: distribute
  input:
    source: plate1(A01)
    destination: plate1(B01)
    amount: 20ul
"""
			val jsobj = JsonUtils.yamlToJson(yaml).asInstanceOf[JsObject]
			val ctx = protocol.processData(jsobj).map({ case (objectToType_m, state) =>
				assert(objectToType_m == Map(
					"plate1" -> "plate"
				))
				assert(state == Strips.State(Set[Strips.Atom](
					Strips.Atom.parse("labware plate1"),
					Strips.Atom.parse("model plate1 plateModel_384_square"),
					Strips.Atom.parse("location plate1 P3")
				)))
			})
			val (data1, _) = ctx.run(data0)
			if (!data1.error_r.isEmpty) {
				println("ERRORS:")
				data1.error_r.foreach(println)
			}
			assert(data1.error_r == Nil)
		}
		
		it("processCommand") {
			val yaml1 = """
object:
  plate1:
    type: plate
    model: plateModel_384_square
    location: P3
command:
  "1":
    command: shakePlate
    input:
      agent: mario
      device: mario__Shaker
      labware: plate1
      site: P3
      program:
        rpm: 200
        duration: 10
"""
			val yaml2 = """
object:
  plate1:
    type: plate
    model: plateModel_384_square
    location: P3
command:
  "1":
    command: shakePlate
    input:
      agent: mario
      labware: plate1
      site: P3
      program:
        rpm: 200
        duration: 10
"""
			val jsobj1 = JsonUtils.yamlToJson(yaml1).asInstanceOf[JsObject]
			val jsobj2 = JsonUtils.yamlToJson(yaml2).asInstanceOf[JsObject]
			val ctx = for {
				_ <- ContextE.evaluate(Converter2.makeImport("shakePlate", "1.0"))
				validation1_l <- protocol.validateDataCommand(jsobj1, "1")
				validation2_l <- protocol.validateDataCommand(jsobj2, "1")
			} yield {
				assert(validation1_l == Nil)
				assert(validation2_l == List(CommandValidation_Param("device")))
			}
			val (data1, _) = ctx.run(data0)
			if (!data1.error_r.isEmpty) {
				println("ERRORS:")
				data1.error_r.foreach(println)
			}
			assert(data1.error_r == Nil)
		}
	}
}