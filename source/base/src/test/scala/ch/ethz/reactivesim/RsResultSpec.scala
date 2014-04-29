package ch.ethz.reactivesim

import org.scalatest.FunSpec

class RsResultSpec extends FunSpec {
	describe("RsResult flatten/sequence") {
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
		
		it("RsResult.flatten() should drop errors") {
			assert(RsResult.flatten(sss) === List(1, 2, 3))
			assert(RsResult.flatten(sww) === List(1, 2, 3))
			assert(RsResult.flatten(swe) === List(1, 2))
			assert(RsResult.flatten(ews) === List(2, 3))
			assert(RsResult.flatten(eee) === List())
		}
		it("RsResult.sequenceDrop() should drop errors but keep warnings from successes") {
			assert(RsResult.sequenceDrop(sss) === RsSuccess(List(1, 2, 3), List()))
			assert(RsResult.sequenceDrop(sww) === RsSuccess(List(1, 2, 3), List("warning 2", "warning 3a", "warning 3b")))
			assert(RsResult.sequenceDrop(swe) === RsSuccess(List(1, 2), List("warning 2")))
			assert(RsResult.sequenceDrop(ews) === RsSuccess(List(2, 3), List("warning 2")))
			assert(RsResult.sequenceDrop(eee) === RsSuccess(List(), List()))
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

	describe("RsResult maps") {
		val l = List(1, 2, 3)
		def fn1(n: Int): RsResult[String] = RsSuccess(n.toString)
		def fn2(n: Int): RsResult[String] = RsSuccess(n.toString, List(s"warning $n"))
		def fn3(n: Int): RsResult[String] = RsError(List(s"error $n"), List(s"warning $n"))
		def fn4(n: Int): RsResult[String] = if (n == 3) RsError(List(s"error $n"), List(s"warning $n")) else RsSuccess(n.toString) 

		it("RsResult.mapFirst() should map until the first error, returning a list of successes with accumulated warnings") {
			assert(RsResult.mapFirst(List(1, 2, 3))(fn1) === RsSuccess(List("1", "2", "3"), List()))
			assert(RsResult.mapFirst(List(1, 2, 3))(fn2) === RsSuccess(List("1", "2", "3"), List("warning 1", "warning 2", "warning 3")))
			assert(RsResult.mapFirst(List(1, 2, 3))(fn3) === RsError(List("error 1"), List("warning 1")))
			assert(RsResult.mapFirst(List(1, 2, 3))(fn4) === RsError(List("error 3"), List("warning 3")))
		}
		
		it("RsResult.mapAll() should return a list of success values with accumulated warnings, but if there were errors it should return the accumulated errors") {
			assert(RsResult.mapAll(List(1, 2, 3))(fn1) === RsSuccess(List("1", "2", "3"), List()))
			assert(RsResult.mapAll(List(1, 2, 3))(fn2) === RsSuccess(List("1", "2", "3"), List("warning 1", "warning 2", "warning 3")))
			assert(RsResult.mapAll(List(1, 2, 3))(fn3) === RsError(List("error 1", "error 2", "error 3"), List("warning 1", "warning 2", "warning 3")))
			assert(RsResult.mapAll(List(1, 2, 3))(fn4) === RsError(List("error 3"), List("warning 3")))
		}
	}
}