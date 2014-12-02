package roboliq.input

import org.scalatest.FunSpec
import roboliq.entities.EntityBase
import roboliq.core.RsSuccess
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsNull

private case class Example1(
	a: Int,
	b: Double,
	c: BigDecimal
)

class Converter2Spec extends FunSpec {
	type ContextEData = ContextXData[EvaluatorState]

	describe("Converter2") {
		val eb = new EntityBase
		val data0 = ContextEData(EvaluatorState(eb))
		val evaluator = new Evaluator();
		val js5 = Converter2.makeNumber(5)
		val js7 = Converter2.makeNumber(7)
		val js12 = Converter2.makeNumber(12)
		val jsSet1 = Converter2.makeMap(Map("a" -> js5, "b" -> js7, "c" -> js12))
		val jsList1 = Converter2.makeList(List(js5, js7, js12))
		
		it("list") {
			val ctx: ContextE[Unit] = for {
				res1 <- Converter2.fromJson[List[Int]](jsList1)
				res2 <- Converter2.fromJson[List[BigDecimal]](jsList1)
			} yield {
				assert(res1 == List(5, 7, 12))
				assert(res2 == List[BigDecimal](5, 7, 12))
			}
			ctx.run(data0)
		}
		
		it("map") {
			val ctx: ContextE[Unit] = for {
				res1 <- Converter2.fromJson[Map[String, Int]](jsSet1)
				res2 <- Converter2.fromJson[Example1](jsSet1)
			} yield {
				assert(res1 == Map("a" -> 5, "b" -> 7, "c" -> 12))
				assert(res2 == Example1(5, 7, 12))
			}
			ctx.run(data0)
		}
		
		it("number") {
			val ctx: ContextE[Unit] = for {
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
			ctx.run(data0)
		}
		
		it("optional number") {
			val ctx: ContextE[Unit] = for {
				res1 <- Converter2.fromJson[Option[Int]](js5)
				res2 <- Converter2.fromJson[Option[Int]](JsNull)
			} yield {
				assert(res1 == Some(5))
				assert(res2 == None)
			}
			ctx.run(data0)
		}
	}
}