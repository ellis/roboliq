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
		val jsX = Converter2.makeSubst("x")
		
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
			val jsAdd = Converter2.makeCall("add", Map("numbers" -> Converter2.makeList(List(js5, js7))))
			val ctx = for {
				res1 <- evaluator.evaluate(jsAdd, Map())
			} yield {
				assert(res1 == js12)
			}
			val data = ContextDataMinimal()
			ctx.run(data)
		}
		
		it("subst") {
			val ctx = for {
				res1 <- evaluator.evaluate(jsX, Map("x" -> js5))
			} yield {
				assert(res1 == js5)
			}
			val data = ContextDataMinimal()
			ctx.run(data)
		}
	}
}