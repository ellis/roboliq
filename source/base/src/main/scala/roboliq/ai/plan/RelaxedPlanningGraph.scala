package roboliq.ai.plan

import Strips._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

object RelaxedPlanningGraph {
	def computeRPGHeuristic(
		problem: Problem
	): Either[String, Int] = {
		val problem2 = problem.relaxed
		for {
			graph <- computeRPG(problem2).right
			h <- extractRPSize(problem2, graph).right
		} yield h
	}
	
	def computeRPG(
		problem: Problem
	): Either[String, BasicPlanningGraph] = {
		val F = new ArrayBuffer[Set[Atom]]
		val A = new ArrayBuffer[Set[Operator]]
		F += problem.state0.atoms
		var t = 0
		while (!problem.goals.pos.forall(F(t).contains)) {
			val state = State(F(t))
			val action_l = problem.domain.getApplicableActions(state)
			F += F(t) ++ action_l.flatMap(_.effects.pos)
			A += problem.domain.getApplicableActions(state).toSet
			t += 1
			if (F(t) == F(t-1))
				return Left("failure")
		}
		Right(BasicPlanningGraph(problem, F.toList, A.toList))
	}
	
	def extractRPSize(problem: Problem, graph: BasicPlanningGraph): Either[String, Int] = {
		val plast = graph.propositionLayer_l.last
		if (!problem.goals.pos.forall(plast.contains))
			return Left("Goals not achieved in graph")
		val goals = problem.goals.pos.toList
		// 
		val firstLevelProp_ll = getFirstLevelPropositions(problem, graph)
		val firstLevelGoal_ll = getFirstLevelGoals(problem, firstLevelProp_ll)
		val firstLevelAction_ll = getFirstLevelActions(graph)

		val selected_l = new HashSet[Operator]
		val pair_l = (firstLevelGoal_ll.reverse, firstLevelProp_ll.reverse, firstLevelAction_ll.reverse).zipped.toList
		extractRPSizeSub(pair_l, Set())
	}
	
	private def extractRPSizeSub(
		pair_l: List[(Set[Atom], Set[Atom], Set[Operator])],
		selected0_l: Set[Operator]
	): Either[String, Int] = pair_l match {
		case Nil => Right(selected0_l.size)
		case (goal_l, _, action_l) :: rest =>
			// Iterator for powerset of actions on this level
			val action_ll = action_l.subsets
			// Find first combination of actions whose effects contain the goals
			val selected_l_? = action_ll.find(action_l => {
				val effect_l = action_l.flatMap(_.effects.pos)
				goal_l.forall(effect_l.contains)
			})
			selected_l_? match {
				case None => return Left("No actions satisfy goal: "+action_l+" "+goal_l)
				case Some(selected_l) =>
					// These preconditions will now need to be fulfilled
					val precond_l = selected_l.flatMap(_.preconds.pos)
					// Add the preconditions to the goal list corresponding to their first level
					val rest2 = rest.map { tuple =>
						val (goal_l, prop_l, action_l) = tuple
						val prop2_l = prop_l.intersect(precond_l)
						(goal_l ++ prop2_l, prop_l, action_l)
					}
					extractRPSizeSub(rest2, selected0_l ++ selected_l)
			}
	}
	
	def getFirstLevelPropositions(problem: Problem, graph: BasicPlanningGraph): List[Set[Atom]] = {
		var seen = new HashSet[Atom]
		for (l <- graph.propositionLayer_l) yield {
			val l2 = l -- seen
			seen ++= l
			l2
		}
	}
	
	def getFirstLevelGoals(problem: Problem, firstLevelProp_ll: List[Set[Atom]]): List[Set[Atom]] = {
		val goals = problem.goals.pos
		firstLevelProp_ll.map(_.filter(goals.contains))
	}
	
	def getFirstLevelActions(graph: BasicPlanningGraph): List[Set[Operator]] = {
		var seen = new HashSet[Operator]
		for (l <- graph.actionLayer_l) yield {
			val l2 = l -- seen
			seen ++= l
			l2
		}
	}
}