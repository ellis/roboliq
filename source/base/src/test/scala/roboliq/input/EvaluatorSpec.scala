package roboliq.input

import org.scalatest.FunSpec
import roboliq.entities.EntityBase
import roboliq.core.RsSuccess
import spray.json.JsNumber
import spray.json.JsObject

class EvaluatorSpec extends FunSpec {
	describe("Evaluator") {
		val eb = new EntityBase
		val evaluator = new Evaluator(eb);
		val js5 = Converter2.makeNumber(5)
		val js7 = Converter2.makeNumber(7)
		val js12 = Converter2.makeNumber(12)
		
		it("number") {
			val ctx = for {
				res1 <- evaluator.evaluate(js12, Map())
				res2 <- evaluator.evaluate(JsNumber(12), Map())
			} yield {
				assert(res1 == js12)
				assert(res2 == js12)
			}
			val data = ContextDataMinimal()
			ctx.run(data)
		}
		
		it("add") {
			val jsAdd = Converter2.makeCall("add", Map("n1" -> js5, "n2" -> js7))
			val ctx = for {
				res1 <- evaluator.evaluate(jsAdd, Map())
			} yield {
				assert(res1 == js12)
			}
			val data = ContextDataMinimal()
			ctx.run(data)
		}
	}
}