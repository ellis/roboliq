package roboliq.input

import org.scalatest.FunSpec
import roboliq.entities.EntityBase
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsNull
import scala.reflect.runtime.universe.TypeTag
import org.scalatest.exceptions.TestFailedException

private case class Example1B(
	a: Int,
	b: Double,
	c: BigDecimal
)

class Converter3Spec extends FunSpec {
	import ContextValueWrapper._
	
	describe("Converter3") {
		val data0 = ResultEData(EvaluatorState())
		val evaluator = new Evaluator();
		val js5 = RjsNumber(5)
		val js7 = RjsNumber(7)
		val js12 = RjsNumber(12)
		val jsWorld = RjsText("World")
		val jsMap1 = RjsMap(Map("a" -> js5, "b" -> js7, "c" -> js12))
		val jsList1 = RjsList(List(js5, js7, js12))
		
		def fromRjs[A: TypeTag](
			rjsval: RjsValue
		): (ResultEData, Option[A]) = {
			Converter3.fromRjs[A](rjsval).run(data0)
		}

		it("RjsValues") {
			assert(fromRjs[RjsValue](js5).value == js5)
			assert(fromRjs[RjsValue](jsWorld).value == jsWorld)
			assert(fromRjs[RjsValue](jsMap1).value == jsMap1)
			assert(fromRjs[RjsValue](jsList1).value == jsList1)
			assert(fromRjs[RjsNumber](js5).value == js5)
			assert(fromRjs[RjsText](jsWorld).value == jsWorld)
			assert(fromRjs[RjsMap](jsMap1).value == jsMap1)
			assert(fromRjs[RjsList](jsList1).value == jsList1)
			assert(fromRjs[RjsInclude](js5).errors.isEmpty == false)
			assert(fromRjs[RjsInclude](jsWorld).errors.isEmpty == false)
			assert(fromRjs[RjsInclude](jsMap1).errors.isEmpty == false)
			assert(fromRjs[RjsInclude](jsList1).errors.isEmpty == false)
		}
		
		it("list") {
			assert(fromRjs[List[Int]](jsList1).value == List(5, 7, 12))
			assert(fromRjs[List[BigDecimal]](jsList1).value == List[BigDecimal](5, 7, 12))
		}
		
		it("map") {
			assert(fromRjs[Map[String, Int]](jsMap1).value == Map("a" -> 5, "b" -> 7, "c" -> 12))
			assert(fromRjs[Example1B](jsMap1).value == Example1B(5, 7, 12))
		}
		
		it("number") {
			val ctx: ResultE[Unit] = for {
				res1 <- Converter3.fromRjs[Int](js5)
				res2 <- Converter3.fromRjs[Integer](js5)
				res3 <- Converter3.fromRjs[Double](js5)
				res4 <- Converter3.fromRjs[BigDecimal](js5)
			} yield {
				assert(res1 == 5)
				assert(res2 == 5)
				assert(res3 == 5)
				assert(res4 == 5)
			}
			ctx.run(data0)
		}
		
		it("optional number") {
			val ctx: ResultE[Unit] = for {
				res1 <- Converter3.fromRjs[Option[Int]](js5)
				res2 <- Converter3.fromRjs[Option[Int]](RjsNull)
			} yield {
				assert(res1 == Some(5))
				assert(res2 == None)
			}
			ctx.run(data0)
		}
	}
}