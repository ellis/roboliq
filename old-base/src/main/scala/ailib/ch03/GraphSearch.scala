package ailib.ch03

import scala.annotation.tailrec
import scala.collection.mutable.HashSet

import ailib.ch03

class GraphSearch[State, Action, Node <: ch03.Node[State]] {
	type Problem = ch03.Problem[State, Action, Node]
	type Frontier = ch03.Frontier[State, Node]
	
	def run(problem: Problem, frontier: Frontier, debug: DebugSpec = new DebugSpec): Option[Node] = {
		assert(frontier.isEmpty)
		val root = problem.root
		val seen = new HashSet[State]
		addToFrontier(frontier, seen, debug, root)
		run2(problem, frontier, seen, debug)
	}
	
	@tailrec final def run2(problem: Problem, frontier: Frontier, seen: HashSet[State], debug: DebugSpec): Option[Node] = {
		if (frontier.isEmpty) {
			None
		}
		else {
			val node = frontier.removeChoice()
			if (debug.printExpanded) {
				println("E: "+node)
			}
			if (problem.goalTest(node.state)) {
				Some(node)
			}
			else {
				for (action <- problem.actions(node.state)) {
					val child = problem.childNode(node, action)
					if (!seen.contains(child.state)) {
						addToFrontier(frontier, seen, debug, child)
					}
				}
				run2(problem, frontier, seen, debug)
			}
		}
	}
	
	private def addToFrontier(frontier: Frontier, seen: HashSet[State], debug: DebugSpec, node: Node) {
		if (debug.printFrontier) {
			println("F: "+node)
		}
		frontier.add(node)
		seen += node.state
	}
}
