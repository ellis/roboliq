package ailib.ch03

import scala.annotation.tailrec
import scala.collection.mutable.HashSet
import scala.collection.mutable.Queue
import scala.collection.mutable.PriorityQueue
import grizzled.slf4j.Logger

import ailib.ch03


class DebugSpec(
	val debug: Boolean = false,
	val printFrontier: Boolean = false,
	val printExpanded: Boolean = false
)

class TreeSearch[State, Action, Node <: ch03.Node[State]] {
	type Problem = ch03.Problem[State, Action, Node]
	type Frontier = ch03.Frontier[State, Node]
	
	private val logger = Logger[this.type]
	
	def run(problem: Problem, frontier: Frontier, debug: DebugSpec = new DebugSpec): Option[Node] = {
		assert(frontier.isEmpty)
		frontier.add(problem.root)
		run2(problem, frontier, debug)
	}
	
	@tailrec
	final def run2(problem: Problem, frontier: Frontier, debug: DebugSpec): Option[Node] = {
		if (frontier.isEmpty) {
			None
		}
		else {
			val node = frontier.removeChoice()
			if (debug.printExpanded) {
				logger.debug("E: "+node)
			}
			if (problem.goalTest(node.state)) {
				Some(node)
			}
			else {
				val child_l = for (action <- problem.actions(node.state)) yield {
					val child = problem.childNode(node, action)
					if (debug.printFrontier) {
						logger.debug("F: "+node)
					}
					child
				}
				frontier.addFIFO(child_l)
				run2(problem, frontier, debug)
			}
		}
	}
}

/*
class BreadthFirstSearch[State, Action] {
	type Node = ailib.ch03.Node[State]
	type Problem = ailib.ch03.Problem[State, Action]
	
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
			if (debug.debug) {
				logger.info("F: "+node.state)
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
			if (debug.debug) {
				logger.info("E: "+node.state)
				//explored ::= node.state
			}
			for (action <- problem.actions(node.state)) {
				val child = problem.childNode(node, action)
				if (!seen.contains(child.state)) {
					addFrontier(child)
					if (problem.goalTest(child.state))
						return Some(child)
				}
			}
		}
		return None
	}
}


class DepthFirstSearch[State, Action] {
	type Node = ailib.ch03.Node[State]
	type Problem = ailib.ch03.Problem[State, Action]
	
	def run(problem: Problem, debug: DebugSpec = new DebugSpec): Option[Node] = {
		var node = new Node(problem.state0, 0, None)
		x(problem, debug, node)
	}
	
	private def x(problem: Problem, debug: DebugSpec, node: Node): Option[Node] = {
		logger.info("E: "+node.state)
		if (problem.goalTest(node.state)) {
			Some(node)
		}
		else {
			val actions = problem.actions(node.state).toList
			y(problem, debug, node, actions)
		}
	}
	
	private def y(problem: Problem, debug: DebugSpec, parent: Node, actions: List[Action]): Option[Node] = {
		actions match {
			case Nil => None
			case action :: rest =>
				val child = problem.childNode(parent, action)
				x(problem, debug, child) match {
					case res @ Some(node) => res
					case None => y(problem, debug, parent, rest)
				}
		}
	}
}
*/
