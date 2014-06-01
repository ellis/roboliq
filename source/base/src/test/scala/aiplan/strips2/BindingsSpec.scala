package aiplan.strips2

import org.scalatest.FunSpec

class BindingsSpec extends FunSpec {
	val b0 = new Bindings(Map(), Map(), Map())
	val b1 = new Bindings(Map("?a" -> Set("a", "b", "c", "d"), "?b" -> Set("a", "b", "c", "d")), Map(), Map())
	val b2 = new Bindings(Map("?a" -> Set("a", "b", "c", "d"), "?b" -> Set("a", "b", "c", "d")), Map("?a" -> Set("?b"), "?b" -> Set("?a")), Map())
	val b3 = new Bindings(Map("?a" -> Set("a"), "?b" -> Set("b", "c", "d")), Map(), Map("?a" -> "a"))
	
	describe("Bindings") {
		it("b0: add variables") {
			assert(b0.addVariables(Map("?a" -> Set("a", "b", "c", "d"), "?b" -> Set("a", "b", "c", "d"))) === Right(b1))
		}
		it("b1: exclude") {
			assert(b1.exclude("?a", "?b") === Right(b2))
		}
		it("b2") {
			assert(b2.assign("?a", "a") === Right(b3))
			assert(b2.assign("?b", "?a") === Left("violation of non-equality constraints"))
		}
		it("b3") {
			assert(b3.assign("?b", "?a") === Left("violation of non-equality constraints"))
			assert(b3.assign("?b", "a") === Left("violation of non-equality constraints"))
		}
	}
}