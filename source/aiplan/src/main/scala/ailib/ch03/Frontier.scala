package ailib.ch03

import scala.annotation.tailrec
import scala.collection.mutable.HashSet
import scala.collection.mutable.Queue
import scala.collection.mutable.PriorityQueue

import ailib.ch03


trait Frontier[State, Node] {
	def isEmpty: Boolean
	def removeChoice(): Node
	def add(node: Node)
}

class DepthFirstFrontier[State, Node] extends Frontier[State, Node] {
	var l = List[Node]()
	
	def isEmpty: Boolean = l.isEmpty
	def removeChoice(): Node = {
		val node = l.head
		l = l.tail
		node
	}
	def add(node: Node) {
		l ::= node
	}
}

class BreadthFirstFrontier[State, Node] extends Frontier[State, Node] {
	val l = new Queue[Node]()
	
	def isEmpty: Boolean = l.isEmpty
	def removeChoice(): Node = l.dequeue()
	def add(node: Node) { l.enqueue(node) }
}

abstract class MinFirstFrontier[State, T, Node <: NodeHeuristic[T]] extends Frontier[State, Node] {
	val l = new PriorityQueue[Node]()(NodeOrdering)
	
	def isEmpty: Boolean = l.isEmpty
	def removeChoice(): Node = l.dequeue()
	def add(node: Node) { l.enqueue(node) }
	
	def compareNodes(a: Node, b: Node): Int
	
	def NodeOrdering: Ordering[Node] = new Ordering[Node] {
		def compare(a: Node, b: Node): Int = compareNodes(a, b)
	}
}
