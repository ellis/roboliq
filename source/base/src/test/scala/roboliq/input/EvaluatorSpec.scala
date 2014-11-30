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
		val jsList57 = Converter2.makeList(List(js5, js7))
		val jsListX7 = Converter2.makeList(List(jsX, js7))
		val jsAdd57 = Converter2.makeCall("add", Map("numbers" -> jsList57))
		val jsAddX7 = Converter2.makeCall("add", Map("numbers" -> jsListX7))
		
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
			check(Map(),
				evaluator.evaluate(jsAdd57) -> js12//,
				//evaluator.evaluate(jsAddX7, Map("x" -> js5)) -> js5
			)
		}

		it("build") {
			val jsBuild1 = Converter2.makeBuild(List(
				"VAR" -> JsObject(Map("NAME" -> JsString("a")) ++ js5.fields),
				"ADD" -> Converter2.makeMap(Map("b" -> js7))
			))
			check(Map(),
				evaluator.evaluate(jsBuild1) -> Converter2.makeList(List(Converter2.makeMap(Map("a" -> js5, "b" -> js7))))
			)
		}
	}
}