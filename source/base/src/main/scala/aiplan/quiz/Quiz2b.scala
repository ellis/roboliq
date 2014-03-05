package aiplan.quiz

import aiplan.strips2.BasicPlanningGraph
import aiplan.strips2.RelaxedPlanningGraph

object Quiz2b7 {
	val domainText = """
(define (domain random-domain-fe)
  (:requirements :strips)
  (:action op1
    :parameters (?x1 ?x2 ?x3)
    :precondition (and (R ?x3 ?x1) (S ?x2 ?x2))
    :effect (and (R ?x1 ?x3) (S ?x3 ?x1) (not (R ?x3 ?x1)) (not (S ?x2 ?x2))))
  (:action op2
    :parameters (?x1 ?x2 ?x3)
    :precondition (and (R ?x1 ?x2) (S ?x2 ?x1) (R ?x2 ?x3))
    :effect (and (R ?x2 ?x2) (S ?x1 ?x2) (not (S ?x2 ?x1)))))
"""
		
	val problemText = """
(define (problem rndpb-fe-1)
  (:domain random-domain-fe)
  (:init
     (S B B)
     (R C C) (R C B) (R B A) (R B C))
  (:goal (and (R B B))))
"""
		
	def run() {
		for {
			domain <- aiplan.strips2.PddlParser.parseDomain(domainText).right
			problem <- aiplan.strips2.PddlParser.parseProblem(domain, problemText).right
		} {
			println("domain:")
			println(domain)
			println()
			println("problem:")
			println(problem)
			println()
			val action_l = domain.getApplicableActions(problem.state0)
			action_l.foreach(println)
			val graph0 = BasicPlanningGraph(problem)
			val graph1 = graph0.run
			graph1.print
			println()
		}
	}
}

object Quiz2b8 {
		
	def run() {
		for {
			domain <- aiplan.strips2.PddlParser.parseDomain(Quiz2b7.domainText).right
			problem <- aiplan.strips2.PddlParser.parseProblem(domain, Quiz2b7.problemText).right
		} {
			println("domain:")
			println(domain)
			println()
			println("problem:")
			println(problem)
			println()
			val graph0 = BasicPlanningGraph(problem)
			val graph1 = graph0.run
			graph1.print
			println()
		}
	}
}


object Quiz2b10 {
		
	def run() {
		for {
			domain <- aiplan.strips2.PddlParser.parseDomain(Quiz2b7.domainText).right
			problem <- aiplan.strips2.PddlParser.parseProblem(domain, Quiz2b7.problemText).right
			graph <- RelaxedPlanningGraph.computeRPG(problem).right
		} {
			graph.print
			println(RelaxedPlanningGraph.computeRPGHeuristic(problem))
		}
	}
}

