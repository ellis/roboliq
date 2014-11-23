package roboliq.input

import org.scalatest.FunSpec
import roboliq.entities.EntityBase
import roboliq.core.RsSuccess
import spray.json.JsNumber
import spray.json.JsObject

private case class Example1(
	a: Int,
	b: Double,
	c: BigDecimal
)

class Converter2Spec extends FunSpec {
	describe("Converter2") {
		val eb = new EntityBase
		val evaluator = new Evaluator(eb);
		val js5 = Converter2.makeNumber(5)
		val js7 = Converter2.makeNumber(7)
		val js12 = Converter2.makeNumber(12)
		val jsSet1 = Converter2.makeMap(Map("a" -> js5, "b" -> js7, "c" -> js12))
		val jsList1 = Converter2.makeList(List(js5, js7, js12))
		
		it("list") {
			val ctx = for {
				res1 <- Converter2.fromJson[List[Int]](jsList1)
				res2 <- Converter2.fromJson[List[BigDecimal]](jsList1)
			} yield {
				assert(res1 == List(5, 7, 12))
				assert(res2 == List[BigDecimal](5, 7, 12))
			}
			val data = ContextDataMinimal()
			ctx.run(data)
		}
		
		it("map") {
			val ctx = for {
				res1 <- Converter2.fromJson[Map[String, Int]](jsSet1)
				res2 <- Converter2.fromJson[Example1](jsSet1)
			} yield {
				assert(res1 == Map("a" -> 5, "b" -> 7, "c" -> 12))
				assert(res2 == Example1(5, 7, 12))
			}
			val data = ContextDataMinimal()
			ctx.run(data)
		}
		
		it("number") {
			val ctx = for {
				res1 <- Converter2.fromJson[Int](js5)
				res2 <- Converter2.fromJson[Integer](js5)
				res3 <- Converter2.fromJson[Double](js5)
				res4 <- Converter2.fromJson[BigDecimal](js5)
			} yield {
				assert(res1 == 5)
				assert(res2 == 5)
				assert(res3 == 5)
				assert(res4 == 5)
			}
			val data = ContextDataMinimal()
			ctx.run(data)
		}
	}
}