package aiplan.hw2

import _root_.aiplan.strips2.Strips._
import aiplan.strips2.BasicPlanningGraph

object Question1 {
	val domain = Domain(
		type_l = List("location", "pile", "robot", "crane", "container"),
		constant_l = List("pallet" -> "container"),
		predicate_l = List(
			"adjacent ?l1:location ?l2:location",
			"attached ?p:pile ?l:location",
			"belong ?k:crane ?l:location",
			"at ?r:robot ?l:location",
			"free ?l:location",
			"loaded ?r:robot ?c:container",
			"unloaded ?r:robot",
			"holding ?k:crane ?c:container",
			"empty ?k:crane",
			"in ?c:container ?p:pile",
			"top ?c:container ?p:pile",
			"on ?k1:container ?k2:container"
		),
		operator_l = List(
			("move ?r:robot ?from:location ?to:location",
				List("adjacent ?from ?to", "at ?r ?from", "free ?to"),
				List("at ?r ?to", "free ?from", "!free ?to", "!at ?r ?from")),
			("load ?k:crane ?l:location ?c:container ?r:robot",
				List("at ?r ?l", "belong ?k ?l", "holding ?k ?c", "unloaded ?r"),
				List("loaded ?r ?c", "!unloaded ?r", "empty ?k", "!holding ?k ?c")),
			("unload ?k:crane ?l:location ?c:container ?r:robot",
				List("belong ?k ?l", "at ?r ?l", "loaded ?r ?c", "empty ?k"),
				List("unloaded ?r", "holding ?k ?c", "!loaded ?r ?c", "!empty ?k")),
			("take ?k:crane ?l:location ?c:container ?else:container ?p:pile",
				List("belong ?k ?l", "attached ?p ?l", "empty ?k", "in ?c ?p", "top ?c ?p", "on ?c ?else"),
				List("holding ?k ?c", "top ?else ?p", "!in ?c ?p", "!top ?c ?p", "!on ?c ?else", "!empty ?k")),
			("put ?k:crane ?l:location ?c:container ?else:container ?p:pile",
				List("belong ?k ?l", "attached ?p ?l", "holding ?k ?c", "top ?else ?p"),
				List("in ?c ?p", "top ?c ?p", "on ?c ?else", "!top ?else ?p", "!holding ?k ?c", "empty ?k"))
		)
	)
	
	val problem = Problem(
		domain = domain,
		object_l = List(
			"r1" -> "robot",
			"l1" -> "location",
			"l2" -> "location",
			"k1" -> "crane",
			"k2" -> "crane",
			"p1" -> "pile",
			"q1" -> "pile",
			"p2" -> "pile",
			"q2" -> "pile",
			"ca" -> "container",
			"cb" -> "container",
			"cc" -> "container",
			"cd" -> "container",
			"ce" -> "container",
			"cf" -> "container"
		),
		state0 = State(Set(
			"adjacent l1 l2",
			"adjacent l2 l1",

			"attached p1 l1",
			"attached q1 l1",
			"attached p2 l2",
			"attached q2 l2",

			"belong k1 l1",
			"belong k2 l2",

			"in ca p1",
			"in cb p1",
			"in cc p1",

			"in cd q1",
			"in ce q1",
			"in cf q1",

			"on ca pallet",
			"on cb ca",
			"on cc cb",

			"on cd pallet",
			"on ce cd",
			"on cf ce",

			"top cc p1",
			"top cf q1",
			"top pallet p2",
			"top pallet q2",

			"at r1 l1",
			"unloaded r1",
			"free l2",

			"empty k1",
			"empty k2"
		)),
		goals = Literals(
			pos = Set(
				"in ca p2",
		        "in cb q2",
		        "in cc p2",
		        "in cd q2",
		        "in ce q2",
		        "in cf q2"
			),
			neg = Set()
		)
	)
	
	def run() {
		println(domain)
		val action_l = domain.getApplicableActions(problem.state0)
		//action_l.foreach(println)
		val graph0 = BasicPlanningGraph(problem)
		val graph1 = graph0.run
		graph1.print
		println()
		for (p <- problem.goals.pos) {
			println(p -> graph1.calcHeuristic(p))
		}
		println()
		println(problem.goals.pos.map(graph1.calcHeuristic))
		println(problem.goals.pos.map(graph1.calcHeuristic).flatten)
		println(problem.goals.pos.map(graph1.calcHeuristic).flatten.sum)
		println(graph1.calcSumHeuristic(problem.goals.pos))
	}
}