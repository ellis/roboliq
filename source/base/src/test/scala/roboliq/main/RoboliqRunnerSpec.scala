package roboliq.main

import org.scalatest.FunSpec
import scala.Vector
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
					RoboliqOptStep_Yaml(YamlContent.protocol2Text)
				)
			)
			assert(RoboliqRunner.process(opt1).run(data0).value == RjsNull)
		}
	}
}