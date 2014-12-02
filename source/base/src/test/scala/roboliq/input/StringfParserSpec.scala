package roboliq.input

import org.scalatest.FunSpec
import roboliq.entities.EntityBase
import roboliq.core.RsSuccess
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsString

class StringfParserSpec extends FunSpec {
	val eb = new EntityBase
	val data0 = ContextEData(EvaluatorState(eb))
	val evaluator = new Evaluator();

	private def check(
		scope: Map[String, JsObject],
		l: (String, String)*
	) {
		val ctx: ContextE[Unit] = for {
			_ <- ContextE.addToScope(scope)
			res_l <- ContextE.mapAll(l) { case (format, _) =>
				StringfParser.parse(format)
			}
		} yield {
			for ((res, (_, expected)) <- res_l zip l) {
				assert(res == expected)
			}
		}
		val (data1, _) = ctx.run(data0)
		if (!data1.error_r.isEmpty) {
			println("ERRORS:")
			data1.error_r.foreach(println)
		}
		assert(data1.error_r == Nil)
	}
	
	describe("StringfParser") {
		val js5 = Converter2.makeNumber(5)
		val jsWorld = Converter2.makeString("World")
		
		it("${x}") {
			val format = "${x}"
			check(Map("x" -> jsWorld), format -> "World")
			check(Map("x" -> js5), format -> "5")
		}
		
		it("Hello, ${x}") {
			val format = "Hello, ${x}"
			check(Map("x" -> jsWorld), format -> "Hello, World")
			check(Map("x" -> js5), format -> "Hello, 5")
		}
		
		it("${x}, hello!") {
			val format = "${x}, hello!"
			check(Map("x" -> jsWorld), format -> "World, hello!")
			check(Map("x" -> js5), format -> "5, hello!")
		}
		
		it("${x} -- ${x}") {
			val format = "${x} -- ${x}"
			check(Map("x" -> jsWorld), format -> "World -- World")
			check(Map("x" -> js5), format -> "5 -- 5")
		}
		
		it("-- ${x} --") {
			val format = "-- ${x} --"
			check(Map("x" -> jsWorld), format -> "-- World --")
			check(Map("x" -> js5), format -> "-- 5 --")
		}
	}
}