package aiplan.strips2

import org.scalatest.FunSpec

class BindingsSpec extends FunSpec {
	val b0 = new Bindings(Map(), Map(), Map())
	
	describe("Bindings") {
		it("should be invalidate based on non-equality constraints") {
			println("b0: "+b0)
			val b1_? = b0.addVariables(Map("?a" -> Set("a", "b", "c", "d"), "?b" -> Set("a", "b", "c", "d")))
			val b1 = b1_?.right.get
			println("b1: "+b1)
			val b2_? = b1.exclude("?a", "?b")
			val b2 = b2_?.right.get
			println("b2: "+b2)
			val b3_? = b2.assign("?a", "a")
			val b3 = b3_?.right.get
			println("b3: "+b3)
			assert(b3.assign("?b", "a") === Left("violation of non-equality constraints"))
		}
	}
}