package roboliq.main

import org.scalatest.FunSpec
import scala.Vector
import roboliq.ai.strips
import roboliq.input.RjsString
import roboliq.input.RjsBasicMap
import roboliq.input.RjsNumber
import roboliq.input.RjsBoolean
import roboliq.input.RjsList
import roboliq.input.RjsValue
import roboliq.input.YamlContent
import roboliq.input.RjsNull
import roboliq.input.ResultEData
import roboliq.input.EvaluatorState
import java.io.File
import roboliq.input.CommandInfo
import roboliq.input.RjsAction
import roboliq.input.RjsMap
import roboliq.input.CommandValidation
import roboliq.input.RjsConverter

class RoboliqRunnerSpec extends FunSpec {
	import roboliq.input.ResultCWrapper._
	import roboliq.input.ResultEWrapper._
	
	describe("RoboliqRunner") {
		it("merge several basic YAML expressions") {
			val map1 = RjsBasicMap(
				"a" -> RjsNumber(1),
				"b" -> RjsString("before")
			)
			val map2 = RjsBasicMap(
				"b" -> RjsString("after"),
				"c" -> RjsBoolean(true)
			)
			val opt1 = RoboliqOpt(
				step_l = Vector(
					RoboliqOptStep_Yaml("OK")
				)
			)
			assert(RoboliqRunner.process(opt1).run().value == RjsString("OK"))

			val opt2 = RoboliqOpt(
				step_l = Vector(
					RoboliqOptStep_Yaml("{a: 1, b: before}"),
					RoboliqOptStep_Yaml("{b: after, c: false}"),
					RoboliqOptStep_Yaml("{c: true, d: [1]}"),
					RoboliqOptStep_Yaml("{d: [2], e: {x: 1}}"),
					RoboliqOptStep_Yaml("{e: {y: 2}}")
				)
			)
			val expected2 = RjsValue.fromYamlText("""{a: 1, b: after, c: true, d: [1, 2], e: {x: 1, y: 2}}""")
			assert(RoboliqRunner.process(opt2).run().value == expected2.run().value)
		}
		
		it("call a builtin function") {
			val opt1 = RoboliqOpt(
				step_l = Vector(
					RoboliqOptStep_Yaml("{TYPE: call, NAME: add, INPUT: {numbers: [1, 2]}}")
				)
			)
			val expected1 = RjsNumber(3)
			assert(RoboliqRunner.process(opt1).run().value == expected1)
		}
		
		it("check protocol without extra data") {
			val data0 = ResultEData(EvaluatorState(searchPath_l = List(new File("testfiles"), new File("base/testfiles"))))
			val opt1 = RoboliqOpt(
				step_l = Vector(
					RoboliqOptStep_Yaml(YamlContent.protocol2Text),
					RoboliqOptStep_Check()
				)
			)

			val result = for {
				result0 <- RoboliqRunner.process(opt1)
				map <- RjsConverter.fromRjs[RjsBasicMap](result0)
				_ = println("map: "+map)
			} yield map.map("commands")

			val expected = Map(
				"1" -> CommandInfo(
					RjsAction("shakePlate", RjsMap("agent" -> RjsString("mario"), "device" -> RjsString("mario.shaker"), "labware" -> RjsString("plate1"), "program" -> RjsBasicMap("duration" -> RjsNumber(10,None), "rpm" -> RjsNumber(200,None)), "site" -> RjsString("P3"))),
					validations = List(
						CommandValidation("agent-has-device mario mario.shaker", precond_? = Some(1)),
						CommandValidation("device-can-site mario.shaker P3", precond_? = Some(2))
					)
				)
			)
			val expected_? = RjsValue.toBasicValue(expected)

			assert(result.run(data0).value == expected_?.run().value)
		}
		
		it("check protocol with lab info") {
			val data0 = ResultEData(EvaluatorState(searchPath_l = List(new File("testfiles"), new File("base/testfiles"))))
			val opt1 = RoboliqOpt(
				step_l = Vector(
					RoboliqOptStep_Yaml(YamlContent.protocol2DataText),
					RoboliqOptStep_Yaml(YamlContent.protocol2Text),
					RoboliqOptStep_Check()
				)
			)
			val expected = RjsBasicMap(
				"1" -> RjsBasicMap(
					"command" -> RjsBasicMap("INPUT" -> RjsBasicMap("agent" -> RjsString("mario"), "device" -> RjsString("mario.shaker"), "labware" -> RjsString("plate1"), "program" -> RjsBasicMap("duration" -> RjsNumber(10,None), "rpm" -> RjsNumber(200,None)), "site" -> RjsString("P3")), "NAME" -> RjsString("shakePlate"), "TYPE" -> RjsString("action")),
					"effects" -> RjsList(List()),
					"successors" -> RjsList(List()),
					"validations" -> RjsList(List())
				),
				"1.1" -> RjsBasicMap(
					"command" -> RjsBasicMap("INPUT" -> RjsBasicMap("agent" -> RjsString("mario"), "device" -> RjsString("mario.shaker"), "labware" -> RjsString("plate1"), "program" -> RjsBasicMap("duration" -> RjsNumber(10,None), "rpm" -> RjsNumber(200,None)), "site" -> RjsString("P3")), "NAME" -> RjsString("ShakerRun"), "TYPE" -> RjsString("instruction")),
					"effects" -> RjsList(List()),
					"successors" -> RjsList(List()),
					"validations" -> RjsList(List())
				)
			)

			val result = for {
				result0 <- RoboliqRunner.process(opt1)
				map <- RjsConverter.fromRjs[RjsBasicMap](result0)
			} yield map.map("commands")
			
			assert(result.run(data0).value == expected)
		}
		
		it("check protocol with evoware agent info") {
			val data0 = ResultEData(EvaluatorState(searchPath_l = List(new File("testfiles"), new File("base/testfiles"))))
			val opt1 = RoboliqOpt(
				step_l = Vector(
					RoboliqOptStep_Yaml(YamlContent.evowareAgentConfig1Text),
					RoboliqOptStep_Yaml(YamlContent.protocol2Text),
					RoboliqOptStep_Check()
				)
			)
			val expected = RjsBasicMap(
				"1" -> RjsBasicMap(
					"command" -> RjsBasicMap("INPUT" -> RjsBasicMap("agent" -> RjsString("mario"), "device" -> RjsString("mario.shaker"), "labware" -> RjsString("plate1"), "program" -> RjsBasicMap("duration" -> RjsNumber(10,None), "rpm" -> RjsNumber(200,None)), "site" -> RjsString("P3")), "NAME" -> RjsString("shakePlate"), "TYPE" -> RjsString("action")),
					"effects" -> RjsList(List()),
					"successors" -> RjsList(List()),
					"validations" -> RjsList(List())
				),
				"1.1" -> RjsBasicMap(
					"command" -> RjsBasicMap("INPUT" -> RjsBasicMap("agent" -> RjsString("mario"), "device" -> RjsString("mario.shaker"), "labware" -> RjsString("plate1"), "program" -> RjsBasicMap("duration" -> RjsNumber(10,None), "rpm" -> RjsNumber(200,None)), "site" -> RjsString("P3")), "NAME" -> RjsString("ShakerRun"), "TYPE" -> RjsString("instruction")),
					"effects" -> RjsList(List()),
					"successors" -> RjsList(List()),
					"validations" -> RjsList(List())
				)
			)

			val result = for {
				result0 <- RoboliqRunner.process(opt1)
				map <- RjsConverter.fromRjs[RjsBasicMap](result0)
			} yield map.map("commands")
			
			assert(result.run(data0).value == expected)
		}
	}
}