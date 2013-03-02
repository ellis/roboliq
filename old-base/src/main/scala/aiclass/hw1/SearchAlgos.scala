/*package aiclass.hw1

import scala.collection.mutable.HashSet
import scala.collection.mutable.Queue

import ailib.ch03
import ailib.ch03._


/*
class TreeSearch[State, Action] {
	type Node = aiclass.hw1.Node[State]
	type Problem = aiclass.hw1.Problem[State, Action]

	def run(state0: State): Option[Node] = {
		val frontierSet 
		var state = state0
		while (true) {
			
		}
	}
}
*/


class BreadthFirstSearch[State, Action] {
	type Node = ch03.Node[State]
	type Problem = ch03.Problem[State, Action]
	
	def run(problem: Problem, debug: DebugSpec = new DebugSpec): Option[Node] = {
		var node = new Node(problem.state0, 0, None)
		if (problem.goalTest(node.state))
			return Some(node)
			
		val frontierQueue = new Queue[Node]
		//val frontierSet = new HashSet[Node]
		val seen = new HashSet[State]
		//var explored = List[State]()
		
		//var frontierOrder = List[Node]()
		
		def addFrontier(node: Node) {
			frontierQueue += node
			//frontierSet += node
			seen += node.state
			if (debug.printFrontier) {
				println("F: "+node.state)
			}
		}
		
		def popFrontier(): Node = {
			val node = frontierQueue.dequeue()
			//frontierSet -= node
			node
		}
		
		addFrontier(node)
		
		while (!frontierQueue.isEmpty) {
			node = popFrontier()
			if (debug.printExpanded) {
				println("E: "+node.state)
				//explored ::= node.state
			}
			if (problem.goalTest(node.state))
				return Some(node)
			for (action <- problem.actions(node.state)) {
				val child = problem.childNode(node, action)
				if (!seen.contains(child.state)) {
					seen += child.state
					addFrontier(child)
				}
			}
		}
		return None
	}
}
*/