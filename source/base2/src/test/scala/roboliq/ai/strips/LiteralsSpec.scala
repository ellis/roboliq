package roboliq.ai.strips

import org.scalatest.FunSpec
import roboliq.ai.plan.Unique

class LiteralsSpec extends FunSpec {
	val a = Atom("a")
	val b = Atom("b")
	val c = Atom("c")
	val d = Atom("d")
	val ap = Literal(a, true)
	val an = Literal(a, false)
	val bp = Literal(b, true)
	val bn = Literal(b, false)
	val ap_bp = Literals(Unique(ap, bp))
	val ap_bn = Literals(Unique(ap, bn))
	
	def literals(n: Int*): Literals = {
		val l = n.toList.zipWithIndex.map { case (t, i) =>
			val name = ('a'.asInstanceOf[Byte] + i).asInstanceOf[Char].toString
			Literal(Atom(name), (t > 0))
		}
		Literals(Unique(l : _*))
	}
	
	describe("Literals") {
		//val objects: RjsBasicMap = RjsBasicMap(),
		//val commands: Map[String, RjsValue] = Map(),
		//val planningDomainObjects: Map[String, String] = Map(),
		//val planningInitialState: strips.Literals = strips.Literals.empty,
		//val processingState_? : Option[ProcessingState] = None
		it("simple constructors") {
			val pos1 = List(a)
			val neg1 = List(b)
			assert(Literals(pos1, neg1) == ap_bn)
			assert(Literals.fromStrings("a", "!b") == ap_bn)
		}
		
		it("holds") {
			val lits1 = Literals(Unique(ap))
			assert(lits1.holds(ap) == true)
			assert(lits1.holds(an) == false)
			assert(lits1.holds(bp) == false)
			assert(lits1.holds(bn) == true)
		}
		
		it("removePos") {
			val lits1 = ap_bp
			assert(lits1.removePos(a) == Literals(Unique(bp)))
			assert(lits1.removePos(b) == Literals(Unique(ap)))
			assert(lits1.removePos(a).removePos(b) == Literals.empty)
			assert(lits1.removePos(b).removePos(a) == Literals.empty)

			val lits2 = ap_bn
			assert(lits2.removePos(a) == Literals(Unique(bn)))
			assert(lits2.removePos(b) == lits2)
		}
		
		it("removeNeg") {
			val lits1 = literals(0, 0)
			assert(lits1.removeNeg(a) == Literals(Unique(bn)))
			assert(lits1.removeNeg(b) == Literals(Unique(an)))
			assert(lits1.removeNeg(a).removeNeg(b) == Literals.empty)
			assert(lits1.removeNeg(b).removeNeg(a) == Literals.empty)

			val lits2 = literals(1, 0)
			assert(lits2.removeNeg(a) == lits2)
			assert(lits2.removeNeg(b) == Literals(Unique(ap)))
		}
		
		it("removeNegs") {
			val lits1 = literals(0, 0, 0, 0)
			val lits2 = literals(1, 1, 0, 0)
			assert(lits1.removeNegs(Set(a, b, c, d)) == Literals.empty)
			assert(lits2.removeNegs(Set(a, b, c, d)) == literals(1, 1))
			assert(lits1.removeNegs(Set(d)) == literals(0, 0, 0))
			assert(lits2.removeNegs(Set(d)) == literals(1, 1, 0))
			assert(lits2.removeNegs(Set(a, b)) == lits2)
		}
		
		it("removeNegs()") {
			val lits1 = literals(0, 0, 0, 0)
			val lits2 = literals(1, 1, 0, 0)
			assert(lits1.removeNegs() == Literals.empty)
			assert(lits2.removeNegs() == literals(1, 1))
		}
		
		it("++") {
			val lits1 = literals(0, 0, 0, 0)
			val lits2 = literals(1, 1, 0, 0)

			val lits3 = lits1 ++ lits2
			assert(lits3.pos == lits2.pos)
			assert(lits3.neg == lits2.neg)
			assert(lits3.l.seq == Vector(Literal(c, false), Literal(d, false), ap, bp))

			val lits4 = lits2 ++ lits1
			assert(lits4.pos.isEmpty)
			assert(lits4.neg == lits1.neg)
			assert(lits4.l.seq == Vector(Literal(c, false), Literal(d, false), an, bn))
		}
		
		it("bind") {
			val lits1 = Literals(Unique(Literal(Atom("ONE", "x", "y"), true)))
			val lits2 = Literals(Unique(Literal(Atom("ONE", "z", "y"), true)))
			assert(lits1.bind(Map("x" -> "z")) == lits2)
			assert(lits1.bind(Map("x" -> "z")) != lits1)
		}
	}
}
