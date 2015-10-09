package roboliq.ai.plan

import scala.language.postfixOps
import roboliq.ai.strips._

case class BasicPlanningGraph(
	problem: Problem,
	propositionLayer_l: List[Set[Atom]],
	actionLayer_l: List[Set[Operator]]
) {
	def next: BasicPlanningGraph = {
		val p0 = propositionLayer_l.last
		val state = State(p0)
		val action_l = problem.domain.getApplicableActions(state).toSet
		val proposition_l = p0 ++ action_l.flatMap(_.effects.pos)
		println("effects: "+action_l.flatMap(_.effects.pos))
		BasicPlanningGraph(
			problem,
			propositionLayer_l ++ List(proposition_l),
			actionLayer_l ++ List(action_l)
		)
	}
	
	def run: BasicPlanningGraph = {
		BasicPlanningGraph.run2(problem, propositionLayer_l.last, this)
	}
	
	def print() {
		printPropositionLayer(0, propositionLayer_l.head)
		for (((actionLayer, propositionLayer), i) <- (actionLayer_l zip propositionLayer_l.tail).zipWithIndex) {
			printActionLayer(i + 1, actionLayer)
			printPropositionLayer(i + 1, propositionLayer)
		}
	}
	
	private def printPropositionLayer(i: Int, ps: Set[Atom]) {
		println(s"P$i (${ps.size})")
		val l = ps.toList.map(_.toString).sorted
		l.foreach(println)
		println()
	}
	
	private def printActionLayer(i: Int, as: Set[Operator]) {
		println(s"A$i (${as.size})")
		val l = as.toList.map(_.toString).sorted
		l.foreach(println)
		println()
	}
	
	/**
	 * @param pos A positive proposition
	 */
	def calcHeuristic(pos: Atom): Option[Int] = {
		val i = propositionLayer_l.indexWhere(_.contains(pos))
		if (i < 0) None
		else Some(i)
	}
	
	def calcSumHeuristic(pos_l: Set[Atom]): Option[Int] = {
		val l = pos_l.toList.map(calcHeuristic)
		// REFACTOR: Use scalaz l.sequence instead of an 'if' statement
		if (l.exists(_ isEmpty)) None else Some(l.flatten.sum)
	}
}

object BasicPlanningGraph {
	def apply(problem: Problem): BasicPlanningGraph =
		BasicPlanningGraph(problem, List(problem.state0.atoms), Nil)

	
	/**
	 * @param p0 Last proposition layer
	 */
	private def run2(problem: Problem, p0: Set[Atom], acc: BasicPlanningGraph): BasicPlanningGraph = {
		val state = State(p0)
		val action_l = problem.domain.getApplicableActions(state).toSet
		val proposition_l = p0 ++ action_l.flatMap(_.effects.pos)
		//println("effects: "+action_l.flatMap(_.effects.pos))
		if (proposition_l.size == p0.size) {
			acc
		}
		else {
			val acc2 = BasicPlanningGraph(
				problem,
				acc.propositionLayer_l ++ List(proposition_l),
				acc.actionLayer_l ++ List(action_l)
			)
			run2(problem, proposition_l, acc2)
		}
	}
}