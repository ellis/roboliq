package roboliq.main

import org.scalatest.FunSpec
import scala.Vector
import roboliq.input.RjsString
import roboliq.input.RjsBasicMap
import roboliq.input.RjsNumber
import roboliq.input.RjsBoolean
import roboliq.input.RjsList

class RoboliqRunnerSpec extends FunSpec {
	import roboliq.input.ResultEWrapper._
	
	describe("RoboliqRunner") {
		it("try1") {
			val map1 = RjsBasicMap(
				"a" -> RjsNumber(1),
				"b" -> RjsString("before")
			)
			val map2 = RjsBasicMap(
				"b" -> RjsString("after"),
				"c" -> RjsBoolean(true)
			)
			val opt1 = RoboliqOpt(
				expression_l = Vector(
					RoboliqOptExpression_Yaml("OK")
				)
			)
			assert(RoboliqRunner.process(opt1).run().value == RjsString("OK"))

			val opt2 = RoboliqOpt(
				expression_l = Vector(
					RoboliqOptExpression_Yaml("{a: 1, b: before}"),
					RoboliqOptExpression_Yaml("{b: after, c: false}"),
					RoboliqOptExpression_Yaml("{c: true, d: [1]}"),
					RoboliqOptExpression_Yaml("{d: [2], e: {x: 1}}"),
					RoboliqOptExpression_Yaml("{e: {y: 2}}")
				)
			)
			val expected2 = RjsBasicMap(
				"a" -> RjsNumber(1),
				"b" -> RjsString("after"),
				"c" -> RjsBoolean(true),
				"d" -> RjsList(RjsNumber(1), RjsNumber(2)),
				"e" -> RjsBasicMap("x" -> RjsNumber(1), "y" -> RjsNumber(2))
			)
			assert(RoboliqRunner.process(opt2).run().value == expected2)
		}
	}
}