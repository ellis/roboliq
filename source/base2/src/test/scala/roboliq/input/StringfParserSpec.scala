package roboliq.input

import org.scalatest.FunSpec
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsString

class StringfParserSpec extends FunSpec {
	val data0 = ResultEData(EvaluatorState())
	val evaluator = new Evaluator();

	private def check(
		scope: RjsMap,
		l: (String, String)*
	) {
		val ctx: ResultE[Unit] = for {
			_ <- ResultE.addToScope(scope)
			res_l <- ResultE.mapAll(l) { case (format, _) =>
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
		val js5 = RjsNumber(5, None)
		val jsWorld = RjsText("World")
		
		it("${x}") {
			val format = "${x}"
			check(RjsMap("x" -> jsWorld), format -> "World")
			check(RjsMap("x" -> js5), format -> "5")
		}
		
		it("Hello, ${x}") {
			val format = "Hello, ${x}"
			check(RjsMap("x" -> jsWorld), format -> "Hello, World")
			check(RjsMap("x" -> js5), format -> "Hello, 5")
		}
		
		it("${x}, hello!") {
			val format = "${x}, hello!"
			check(RjsMap("x" -> jsWorld), format -> "World, hello!")
			check(RjsMap("x" -> js5), format -> "5, hello!")
		}
		
		it("${x} -- ${x}") {
			val format = "${x} -- ${x}"
			check(RjsMap("x" -> jsWorld), format -> "World -- World")
			check(RjsMap("x" -> js5), format -> "5 -- 5")
		}
		
		it("-- ${x} --") {
			val format = "-- ${x} --"
			check(RjsMap("x" -> jsWorld), format -> "-- World --")
			check(RjsMap("x" -> js5), format -> "-- 5 --")
		}
	}
}