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

class EvaluatorSpec extends FunSpec {
	val eb = new EntityBase
	val data0 = ContextEData(EvaluatorState(eb, searchPath_l = List(new File("testfiles"))))
	val evaluator = new Evaluator();

	private def check(
		scope: RjsMap,
		l: (RjsValue, RjsValue)*
	) {
		val ctx: ContextE[Unit] = for {
			_ <- ContextE.addToScope(scope)
			res_l <- ContextE.map(l) { case (jsobj, _) => evaluator.evaluate(jsobj) }
		} yield {
			for ((res, expected) <- res_l zip l.map(_._2))
				assert(res == expected)
		}
		val (data1, _) = ctx.run(data0)
		if (!data1.error_r.isEmpty) {
			println("ERRORS:")
			data1.error_r.foreach(println)
		}
		assert(data1.error_r == Nil)
	}
	
	describe("Evaluator") {
		val js1 = RjsNumber(1)
		val js5 = RjsNumber(5)
		val js6 = RjsNumber(6)
		val js7 = RjsNumber(7)
		val js12 = RjsNumber(12)
		val jsX = RjsSubst("x")
		val jsY = RjsSubst("y")
		val jsList57 = RjsList(List(js5, js7))
		val jsListX7 = RjsList(List(jsX, js7))
		val jsListXY = RjsList(List(jsX, jsY))
		val jsAdd57 = RjsCall("add", RjsMap("numbers" -> jsList57))
		val jsAddX7 = RjsCall("add", RjsMap("numbers" -> jsListX7))
		val jsAddXY = RjsCall("add", RjsMap("numbers" -> jsListXY))
		val jsWorld = RjsText("World")
		val jsHelloX = RjsFormat("Hello, ${x}")
		val jsLambdaInc = RjsLambda(List("x"), RjsCall("add", RjsMap("numbers" -> RjsList(List(jsX, js1)))))
		
		it("number") {
			check(RjsMap(),
				js12 -> js12,
				RjsNumber(12) -> js12
			)
		}
		
		it("string") {
			check(RjsMap(),
				jsWorld -> jsWorld,
				RjsText("World") -> jsWorld
			)
		}
		
		it("subst") {
			check(RjsMap("x" -> js5),
				jsX -> js5
			)
		}
		
		it("stringf") {
			check(RjsMap("x" -> jsWorld),
				jsHelloX -> RjsText("Hello, World")
			)
			check(RjsMap("x" -> js5),
				jsHelloX -> RjsText("Hello, 5")
			)
		}

		it("list") {
			check(RjsMap(),
				jsList57 -> jsList57
			)
		}
		
		it("list with subst") {
			check(RjsMap("x" -> js5),
				jsListX7 -> jsList57
			)
		}
		
		it("list with call with subst") {
			val jsList3 = RjsList(List(js5, jsAddX7))
			check(RjsMap("x" -> js5),
				jsList3 -> RjsList(List(js5, js12))
			)
		}

		it("add") {
			check(RjsMap("x" -> js5),
				jsAdd57 -> js12,
				jsAddX7 -> js12
			)
		}
		
		it("let") {
			val jsLet1 = RjsLet(
				List(RjsDefine("x", js5), RjsDefine("y", js7)),
				jsAddXY
			)
			check(RjsMap(),
				jsLet1 -> js12
			)
		}
		
		it("lambda") {
			// Lamba objects should remain unchanged
			check(RjsMap(),
				jsLambdaInc -> jsLambdaInc
			)
			// Call a lambda
			check(RjsMap("inc" -> jsLambdaInc),
				RjsCall("inc", RjsMap("x" -> js5)) -> js6
			)
		}

		it("build") {
			val jsBuild1 = RjsBuild(List(
				RjsBuildItem_VAR("a", js5),
				RjsBuildItem_ADD(RjsMap("b" -> js7))
			))
			val jsBuild2 = RjsBuild(List(
				RjsBuildItem_VAR("a", js5),
				RjsBuildItem_ADD(RjsMap("b" -> js7)),
				RjsBuildItem_ADD(RjsMap("b" -> js12)),
				RjsBuildItem_VAR("a", js7),
				RjsBuildItem_ADD(RjsMap("b" -> js12))
			))
			check(RjsMap(),
				jsBuild1 -> RjsList(List(RjsMap("a" -> js5, "b" -> js7))),
				jsBuild2 -> RjsList(List(
					RjsMap("a" -> js5, "b" -> js7),
					RjsMap("a" -> js5, "b" -> js12),
					RjsMap("a" -> js7, "b" -> js12)
				))
			)
		}
		
		it("include") {
			check(RjsMap(),
				RjsInclude("include1.yaml") -> js1
			)
		}
		
		it("import") {
			check(RjsMap(),
				RjsList(List(
					RjsImport("inc", "1.0"),
					RjsCall("inc", RjsMap("x" -> js5))
				)) -> RjsList(List(js6))
			)
		}
		
		it("instruction list") {
			val jsInstructionList = RjsList(List(
				RjsInstruction("Instruction1", RjsMap("agent" -> RjsString("one"))),
				RjsInstruction("Instruction2", RjsMap("agent" -> RjsString("two")))
			))
			check(RjsMap(),
				jsInstructionList -> jsInstructionList
			)
		}
		
		it("create protocol") {
			val yaml = """
labware:
  plate1:
    model: plateModel_384_square
    location: P3

substance:
  water: {}
  dye: {}

source:
  dyeLight:
    well: trough1(A01|H01)
    substance:
    - name: dye
      amount: 1/10
    - name: water

command:
- TYPE: action
  NAME: distribute
  INPUT:
    source: dyeLight
    destination: plate1(B01)
    amount: 20ul
"""
			val jsval = JsonUtils.yamlToJson(yaml)
			println(jsval)
		}
	}
}