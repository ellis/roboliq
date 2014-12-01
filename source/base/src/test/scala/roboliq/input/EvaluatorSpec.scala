package roboliq.input

import org.scalatest.FunSpec
import roboliq.entities.EntityBase
import roboliq.core.RsSuccess
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsString

class EvaluatorSpec extends FunSpec {
	val eb = new EntityBase
	val data0 = ContextEData(EvaluatorState(eb, Map()))
	val evaluator = new Evaluator();

	private def check(
		scope: Map[String, JsObject],
		l: (ContextE[JsObject], JsObject)*
	) {
		val ctx: ContextE[Unit] = for {
			_ <- ContextE.addToScope(scope)
			res_l <- ContextE.map(l) { _._1 }
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
		val js5 = Converter2.makeNumber(5)
		val js7 = Converter2.makeNumber(7)
		val js12 = Converter2.makeNumber(12)
		val jsX = Converter2.makeSubst("x")
		val jsY = Converter2.makeSubst("y")
		val jsList57 = Converter2.makeList(List(js5, js7))
		val jsListX7 = Converter2.makeList(List(jsX, js7))
		val jsListXY = Converter2.makeList(List(jsX, jsY))
		val jsAdd57 = Converter2.makeCall("add", Map("numbers" -> jsList57))
		val jsAddX7 = Converter2.makeCall("add", Map("numbers" -> jsListX7))
		val jsAddXY = Converter2.makeCall("add", Map("numbers" -> jsListXY))
		val jsWorld = Converter2.makeString("World")
		val jsHelloX = Converter2.makeStringf("Hello, ${x}")
		
		it("number") {
			check(Map(),
				evaluator.evaluate(js12) -> js12,
				evaluator.evaluate(JsNumber(12)) -> js12
			)
		}
		
		it("subst") {
			check(Map("x" -> js5),
				evaluator.evaluate(jsX) -> js5
			)
		}
		
		it("list") {
			check(Map(),
				evaluator.evaluate(jsList57) -> jsList57
			)
		}
		
		it("list with subst") {
			check(Map("x" -> js5),
				evaluator.evaluate(jsListX7) -> jsList57
			)
		}
		
		it("list with call with subst") {
			val jsList3 = Converter2.makeList(List(js5, jsAddX7))
			check(Map("x" -> js5),
				evaluator.evaluate(jsList3) -> Converter2.makeList(List(js5, js12))
			)
		}

		it("add") {
			check(Map("x" -> js5),
				evaluator.evaluate(jsAdd57) -> js12,
				evaluator.evaluate(jsAddX7) -> js12
			)
		}
		
		it("let") {
			val jsLet1 = Converter2.makeLet(
				List("x" -> js5, "y" -> js7),
				jsAddXY
			)
			check(Map(),
				evaluator.evaluate(jsLet1) -> js12
			)
		}

		it("build") {
			val jsBuild1 = Converter2.makeBuild(List(
				"VAR" -> JsObject(Map("NAME" -> JsString("a")) ++ js5.fields),
				"ADD" -> Converter2.makeMap(Map("b" -> js7))
			))
			val jsBuild2 = Converter2.makeBuild(List(
				"VAR" -> JsObject(Map("NAME" -> JsString("a")) ++ js5.fields),
				"ADD" -> Converter2.makeMap(Map("b" -> js7)),
				"ADD" -> Converter2.makeMap(Map("b" -> js12)),
				"VAR" -> JsObject(Map("NAME" -> JsString("a")) ++ js7.fields),
				"ADD" -> Converter2.makeMap(Map("b" -> js12))
			))
			check(Map(),
				evaluator.evaluate(jsBuild1) -> Converter2.makeList(List(Converter2.makeMap(Map("a" -> js5, "b" -> js7)))),
				evaluator.evaluate(jsBuild2) -> Converter2.makeList(List(
					Converter2.makeMap(Map("a" -> js5, "b" -> js7)),
					Converter2.makeMap(Map("a" -> js5, "b" -> js12)),
					Converter2.makeMap(Map("a" -> js7, "b" -> js12))
				))
			)
		}
		
		it("stringf") {
			check(Map("x" -> jsWorld),
				evaluator.evaluate(jsHelloX) -> Converter2.makeString("Hello, World")
			)
			check(Map("x" -> js5),
				evaluator.evaluate(jsHelloX) -> Converter2.makeString("Hello, 5")
			)
		}
	}
}