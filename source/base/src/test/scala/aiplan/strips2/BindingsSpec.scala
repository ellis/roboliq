package aiplan.strips2

import org.scalatest.FunSpec

class BindingsSpec extends FunSpec {
	val empty = new Bindings(Map(), Map(), Map())
	val ab = new Bindings(Map("?a" -> Set("a", "b", "c", "d"), "?b" -> Set("a", "b", "c", "d")), Map(), Map())
	val abEq = new Bindings(Map("?a" -> Set("a", "b", "c", "d"), "?b" -> Set("?a")), Map(), Map("?b" -> "?a"))
	val abEq1 = new Bindings(Map("?a" -> Set("a"), "?b" -> Set("?a")), Map(), Map("?a" -> "a", "?b" -> "?a"))
	val abNe = new Bindings(Map("?a" -> Set("a", "b", "c", "d"), "?b" -> Set("a", "b", "c", "d")), Map("?a" -> Set("?b"), "?b" -> Set("?a")), Map())
	val abNe1 = new Bindings(Map("?a" -> Set("a"), "?b" -> Set("b", "c", "d")), Map(), Map("?a" -> "a"))
	
	describe("Bindings") {
		it("empty -> ab: add variables") {
			assert(empty.addVariables(Map("?a" -> Set("a", "b", "c", "d"), "?b" -> Set("a", "b", "c", "d"))) === Right(ab))
		}
		it("ab -> abEq: set ?a == ?b") {
			assert(ab.assign("?a", "?b") === Right(abEq))
		}
		it("abEq -> abEq1: assign ?a := a") {
			assert(abEq.assign("?a", "a") === Right(abEq1))
		}
		it("ab -> abNe: set ?a != ?b") {
			assert(ab.exclude("?a", "?b") === Right(abNe))
		}
		it("abNe: assign ?a == ?b, which should fail") {
			assert(abNe.assign("?a", "?b") === Left("cannot let `?a == ?b`, because of a previous inequality constraint"))
		}
		it("abNe -> abNe1: assign ?a := a") {
			assert(abNe.assign("?a", "a") === Right(abNe1))
		}
		it("abNe1: assign ?b == ?a / ?b := a, which should fail") {
			assert(abNe1.assign("?a", "?b") === Left("cannot let `?b := a`, because it is not one of the options Set(b, c, d)"))
			assert(abNe1.assign("?b", "a") === Left("cannot let `?b := a`, because it is not one of the options Set(b, c, d)"))
		}
	}
}