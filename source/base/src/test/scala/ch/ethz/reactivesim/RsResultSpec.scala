package ch.ethz.reactivesim

import org.scalatest.FunSpec

class RsResultSpec extends FunSpec {
	describe("RsResult") {
		def w(n: Int): RsResult[Int] = RsSuccess(n, List(s"warning $n"))
		
		val s1 = RsSuccess(1)
		val s2 = RsSuccess(2)
		val s3 = RsSuccess(3)
		val w2 = RsSuccess(2, List("warning 2"))
		val w3 = RsSuccess(3, List("warning 3a", "warning 3b"))
		val e1 = RsError[Int]("error 1")
		val e2 = RsError[Int](List("error 2"), List("warning 2"))
		val e3 = RsError[Int](List("error 3"), List("warning 3a", "warning 3b"))
		val sss = List[RsResult[Int]](s1, s2, s3)
		val sww = List[RsResult[Int]](s1, w2, w3)
		val swe = List[RsResult[Int]](s1, w2, e3)
		val ews = List[RsResult[Int]](e1, w2, s3)
		val eee = List[RsResult[Int]](e1, e2, e3)
		// sss
		// w1w2w3
		// eee
		it("RsResult.flatten() should drop errors") {
			assert(RsResult.flatten(sss) === List(1, 2, 3))
			assert(RsResult.flatten(sww) === List(1, 2, 3))
			assert(RsResult.flatten(swe) === List(1, 2))
			assert(RsResult.flatten(ews) === List(2, 3))
			assert(RsResult.flatten(eee) === List())
		}
		it("RsResult.sequenceDrop() should drop errors but keep warnings from successes") {
			assert(RsResult.sequenceDrop(List[RsResult[Int]](RsError("a"), RsError("b"), RsError("c"))) === RsSuccess(List(), List()))
			assert(RsResult.sequenceDrop(List[RsResult[Int]](RsSuccess(1), RsError(List("woops"), List("warning 2a", "warning 2b")), RsSuccess(3, List("warning 3a", "warning 3b")))) === RsSuccess(List(1, 3), List("warning 3a", "warning 3b")))
		}
		it("RsResult.sequenceFirst() should return first error or list of successes with accumulated warnings") {
			assert(RsResult.sequenceFirst(sss) === RsSuccess(List(1, 2, 3), List()))
			assert(RsResult.sequenceFirst(sww) === RsSuccess(List(1, 2, 3), List("warning 2", "warning 3a", "warning 3b")))
			assert(RsResult.sequenceFirst(swe) === e3)
			assert(RsResult.sequenceFirst(ews) === e1)
			assert(RsResult.sequenceFirst(eee) === e1)
		}
		it("RsResult.sequenceAll() should return accumulated errors or list of successes with accumulated warnings") {
			assert(RsResult.sequenceAll(sss) === RsSuccess(List(1, 2, 3), List()))
			assert(RsResult.sequenceAll(sww) === RsSuccess(List(1, 2, 3), List("warning 2", "warning 3a", "warning 3b")))
			assert(RsResult.sequenceAll(swe) === e3)
			assert(RsResult.sequenceAll(ews) === e1)
			assert(RsResult.sequenceAll(eee) === RsError(List("error 1", "error 2", "error 3"), List("warning 2", "warning 3a", "warning 3b")))
		}
	}
}