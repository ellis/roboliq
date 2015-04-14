package roboliq.ai.strips

import org.scalatest.FunSpec
import roboliq.ai.plan.Unique

class LiteralsSpec extends FunSpec {
	val a = Atom("a")
	val b = Atom("b")
	val c = Atom("c")
	val ap = Literal(a, true)
	val an = Literal(a, false)
	val bp = Literal(b, true)
	val bn = Literal(b, false)
	val ap_bn = Unique(ap, bn)
	
	describe("Literals") {
		//val objects: RjsBasicMap = RjsBasicMap(),
		//val commands: Map[String, RjsValue] = Map(),
		//val planningDomainObjects: Map[String, String] = Map(),
		//val planningInitialState: strips.Literals = strips.Literals.empty,
		//val processingState_? : Option[ProcessingState] = None
		it("simple constructors") {
			val pos1 = List(a)
			val neg1 = List(b)
			val ap_bn = Unique(ap, bn)
			assert(Literals(pos1, neg1) == Literals(ap_bn))
			assert(Literals.fromStrings("a", "!b") == Literals(ap_bn))
		}
		
		it("holds") {
			val lits1 = Literals(Unique(ap))
			assert(lits1.holds(ap) == true)
			assert(lits1.holds(an) == false)
			assert(lits1.holds(bp) == false)
			assert(lits1.holds(bn) == true)
		}
		
		it("removePos") {
			assert(false) // CONTINUE HERE
		}
	}
/*
class Literals private (val l: Unique[Literal], val pos: Set[Atom], val neg: Set[Atom]) {
	/**
	 * Check whether the given literal holds among the literals.
	 * If the literal is positive, 'holding' means that the set of positive atom contains the literal's atom.
	 * Otherwise, 'holding' means that the set of positive atoms does not contain the literal's atom.
	 */
	def holds(literal: Literal): Boolean = {
		if (literal.pos)
			pos.contains(literal.atom)
		else
			!pos.contains(literal.atom)
	}
	
	//def ++(that: Literals) = Literals(pos ++ that.pos, neg ++ that.neg)
	def removePos(atom: Atom): Literals = {
		val lit = Literal(atom, true)
		new Literals(l - lit, pos - atom, neg)
	}
	def removeNeg(atom: Atom): Literals = {
		val lit = Literal(atom, false)
		new Literals(l - lit, pos, neg - atom)
	}
	def removeNegs(atom_l: Set[Atom]): Literals = {
		val lit_l = atom_l.map(atom => Literal(atom, false))
		new Literals(l -- lit_l, pos, neg -- atom_l)
	}
	/**
	 * Remove all negative literals
	 */
	def removeNegs(): Literals = {
		val l2 = l.filter(_.pos)
		new Literals(l2, pos, Set())
	}
	
	def ++(that: Literals): Literals = {
		// TODO: consider removing superfluous values from l
		//val removePos = pos intersect that.neg
		//val removeNeg = neg intersect that.pos
		new Literals(l ++ that.l, pos -- that.neg ++ that.pos, neg ++ that.neg -- that.pos)
	}
	
	def bind(map: Map[String, String]): Literals =
		Literals(l.map(_.bind(map)))
		
	override def toString: String = {
		l.mkString("Literals(", ",", ")")
	}
	
	override def equals(that: Any) = that match {
		case that2: Literals => this.pos == that2.pos && this.neg == that2.neg
		case _ => false
	}
}

object Literals {
	val empty = new Literals(Unique(), Set(), Set())
	def apply(l: Unique[Literal]): Literals = {
		val pos: Unique[Atom] = l.collect { case Literal(atom, true) => atom }
		val neg: Unique[Atom] = l.collect { case Literal(atom, false) => atom }
		// TODO: consider removing pos from neg
		// TODO: consider removing duplicates from l, as well as negatives for which there is a positive
		new Literals(l, pos.toSet, neg.toSet)
	}
	def apply(pos: List[Atom], neg: List[Atom]): Literals = {
		val pos1 = pos.map(atom => Literal(atom, true))
		val neg1 = neg.map(atom => Literal(atom, false))
		// TODO: consider removing pos1 from neg1
		val l = Unique((pos1 ++ neg1) : _*)
		new Literals(l, pos.toSet, neg.toSet)
	}
	def apply(state: State): Literals = {
		apply(state.atoms.toList, Nil)
	}
	def fromStrings(l: String*): Literals = {
		val l2 = Unique[Literal](l.map(Literal.parse) : _*)
		apply(l2)
	}
}
 */
}