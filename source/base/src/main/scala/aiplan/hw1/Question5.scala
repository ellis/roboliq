package aiplan.hw1

import _root_.aiplan.strips._

object Question5 {
	/*
	  DOMAIN:
	  (:action op1
	    :parameters (?x1 ?x2 ?x3)
	    :precondition (and (S ?x1 ?x2) (R ?x3 ?x1))
	    :effect (and (S ?x2 ?x1) (S ?x1 ?x3) (not (R ?x3 ?x1))))
	  (:action op2
	    :parameters (?x1 ?x2 ?x3)
	    :precondition (and (S ?x3 ?x1) (R ?x2 ?x2))
	    :effect (and (S ?x1 ?x3) (not (S ?x3 ?x1)))))%                                     
	*/
	val op1 = Operator(
		"op1",
		Seq("?x1", "?x2", "?x3"),
		Set(
			Literal(Atom("S", Seq("?x1", "?x2")), true),
			Literal(Atom("R", Seq("?x3", "?x1")), true)
		),
		Set(
			Literal(Atom("S", Seq("?x2", "?x1")), true),
			Literal(Atom("S", Seq("?x1", "?x3")), true),
			Literal(Atom("R", Seq("?x3", "?x1")), false)
		)
	)
	val op2 = Operator(
		"op2",
		Seq("?x1", "?x2", "?x3"),
		Set(
			Literal(Atom("S", Seq("?x3", "?x1")), true),
			Literal(Atom("R", Seq("?x2", "?x2")), true)
		),
		Set(
			Literal(Atom("S", Seq("?x1", "?x3")), true),
			Literal(Atom("S", Seq("?x3", "?x1")), false)
		)
	)
	/*
	 * PROBLEM:
	  (:init
	     (S B B) (S C B) (S A C)
	     (R B B) (R C B))
	  (:goal (and (S A A))))%                                                              
	 */
	val state0 = State(Set[Atom](
		Atom("S", Seq("B", "B")),
		Atom("S", Seq("C", "B")),
		Atom("S", Seq("A", "C")),
		Atom("R", Seq("B", "B")),
		Atom("R", Seq("C", "B"))
	))
	val goals = Set[Literal](
		Literal(Atom("S", Seq("A", "A")), true)
	)
	
	def run() {
		val objects = Set("A", "B", "C")
		Stuff.getRelevantActions(op1, goals, objects).foreach(println)
		Stuff.getRelevantActions(op2, goals, objects).foreach(println)
	}
}