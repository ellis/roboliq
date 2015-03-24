package roboliq.input

import org.scalatest.FunSpec
import roboliq.entities.EntityBase
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsNull
import scala.reflect.runtime.universe.TypeTag
import org.scalatest.exceptions.TestFailedException

private case class RjsConverterSpecExample(
	a: Int,
	b: Double,
	c: BigDecimal
)

class RjsConverterSpec extends FunSpec {
	import ResultEWrapper._
	
	describe("RjsConverter") {
		val data0 = ResultEData(EvaluatorState())
		val evaluator = new Evaluator()
		val js5 = RjsNumber(5)
		val js7 = RjsNumber(7)
		val js12 = RjsNumber(12)
		val jsWorld = RjsText("World")
		val jsMap1 = RjsMap(Map("a" -> js5, "b" -> js7, "c" -> js12))
		val jsList1 = RjsList(List(js5, js7, js12))
		val jsSubstX = RjsSubst("x")
		
		def fromRjs[A: TypeTag](
			rjsval: RjsValue
		): (ResultEData, Option[A]) = {
			RjsConverter.fromRjs[A](rjsval).run(data0)
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
			assert(fromRjs[RjsInclude](jsMap1).errors != Nil)
			assert(fromRjs[RjsInclude](jsList1).errors.isEmpty == false)
		}
		
		it("list") {
			assert(fromRjs[List[Int]](jsList1).value == List(5, 7, 12))
			assert(fromRjs[List[BigDecimal]](jsList1).value == List[BigDecimal](5, 7, 12))
		}
		
		it("map") {
			assert(fromRjs[Map[String, Int]](jsMap1).value == Map("a" -> 5, "b" -> 7, "c" -> 12))
			assert(fromRjs[RjsConverterSpecExample](jsMap1).value == RjsConverterSpecExample(5, 7, 12))
		}
		
		it("number") {
			assert(fromRjs[Int](js5).value == 5)
			assert(fromRjs[Integer](js5).value == 5)
			assert(fromRjs[Float](js5).value == 5.0f)
			assert(fromRjs[Double](js5).value == 5.0)
			assert(fromRjs[BigDecimal](js5).value == 5)
		}
		
		it("optional number") {
			assert(fromRjs[Option[Int]](js5).value == Some(5))
			assert(fromRjs[Option[Int]](RjsNull).value == None)
		}
		
		it("RjsSubst -> String") {
			val res = for {
				_ <- ResultE.addToScope("x", jsWorld)
				s <- RjsConverter.fromRjs[String](jsSubstX)
			} yield s
			assert(res.run().value == "World")
		}
		
		it("RjsProtocol") {
			assert(RjsConverter.yamlStringToRjs[RjsProtocol](YamlContent.protocol1Text).run().value == YamlContent.protocol1)
		}
	}
}