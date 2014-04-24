package ch.ethz.reactivesim

import org.scalatest.FunSpec

class RsResultSpec extends FunSpec {
	describe("RsResult") {
		it("RsResult.flatten() should drop errors") {
			assert(RsResult.flatten(List[RsResult[Int]](RsError("a"), RsError("b"), RsError("c"))) === List())
			assert(RsResult.flatten(List[RsResult[Int]](RsSuccess(1), RsError("woops"), RsSuccess(3))) === List(1, 3))
		}
		it("RsResult.sequenceDrop() should drop errors but keep warnings from successes") {
			assert(RsResult.sequenceDrop(List[RsResult[Int]](RsError("a"), RsError("b"), RsError("c"))) === RsSuccess(List(), List()))
			assert(RsResult.sequenceDrop(List[RsResult[Int]](RsSuccess(1), RsError(List("woops"), List("warning 2a", "warning 2b")), RsSuccess(3, List("warning 3a", "warning 3b")))) === RsSuccess(List(1, 3), List("warning 3a", "warning 3b")))
		}
	}
}