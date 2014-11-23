package roboliq.input

import org.scalatest.FunSpec
import roboliq.entities.EntityBase
import roboliq.core.RsSuccess
import spray.json.JsNumber
import spray.json.JsObject

class EvaluatorSpec extends FunSpec {
	val eb = new EntityBase
	val evaluator = new Evaluator(eb);

	private def check(l: (ContextT[JsObject], JsObject)*) {
		val ctx = for {
			res_l <- ContextT.map(l) { _._1 }
		} yield {
			for ((res, expected) <- res_l zip l.map(_._2))
				assert(res == expected)
		}
		val data0 = ContextDataMinimal()
		val (data1_~, _) = ctx.run(data0)
		val data1 = data1_~.asInstanceOf[ContextDataMinimal]
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
			check(
				evaluator.evaluate(js12, Map()) -> js12,
				evaluator.evaluate(JsNumber(12), Map()) -> js12
			)
		}
		
		it("subst") {
			check(
				evaluator.evaluate(jsX, Map("x" -> js5)) -> js5
			)
		}
		
		it("list") {
			check(
				evaluator.evaluate(jsList57, Map()) -> jsList57
			)
		}
		
		it("list with subst") {
			check(
				evaluator.evaluate(jsListX7, Map("x" -> js5)) -> jsList57
			)
		}
		
		it("list with call with subst") {
			val jsList3 = Converter2.makeList(List(js5, jsAddX7))
			check(
				evaluator.evaluate(jsList3, Map("x" -> js5)) -> Converter2.makeList(List(js5, js12))
			)
		}

		it("add") {
			check(
				evaluator.evaluate(jsAdd57, Map()) -> js12//,
				//evaluator.evaluate(jsAddX7, Map("x" -> js5)) -> js5
			)
		}
	}
}