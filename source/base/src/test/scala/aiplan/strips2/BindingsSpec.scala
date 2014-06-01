package aiplan.strips2

import org.scalatest.FunSpec

class BindingsSpec extends FunSpec {
	val b0 = new Bindings(Map(), Map())
	
	describe("Bindings") {
		it("should be invalidate based on non-equality constraints") {
			val b1_? = b0.addVariables(Map("1:?a" -> Set("a", "b", "c", "d"), "1:?b" -> Set("a", "b", "c", "d")))
			println("b1_?: "+b1_?)
			val b1 = b1_?.right.get
			val b2_? = b1.exclude("1:?a", "?b")
			val b2 = b2_?.right.get
			val b3_? = b2.assign("1:?a", "a")
			val b3 = b3_?.right.get
			assert(b3.assign("1:?b", "a") === Left("violation of non-equality constraints"))
		}
	}
}