package aiplan.hw1

import _root_.ailib.ch03
import _root_.ailib.ch03._


object Question1 {
	def swap(a: Seq[Int], i1: Int, i2: Int): Seq[Int] = {
		val b = Array[Int](a : _*)
		b(i1) = a(i2)
		b(i2) = a(i1)
		//println(a.mkString("[", " ", "]") ++ " -> " ++ i1.toString ++ i2.toString ++ b.mkString("[", " ", "]"))
		b
	}
	
	def distance(a: Seq[Int]): Int = {
		(1 until 9).map(index1 => {
			val row1 = index1 / 3
			val col1 = index1 % 3
			val index2 = a.indexOf(index1)
			val row2 = index2 / 3
			val col2 = index2 % 3
			math.abs(row1 - row2) + math.abs(col1 - col2)
		}).reduce(_ + _)
	}
	
	case class State(a: Seq[Int]) {
		//if (a.sorted != Array(0,1,2,3,4,5,6,7,8))
		//	println(a.toList.sorted)
		assert(a.sorted == Seq(0,1,2,3,4,5,6,7,8))
		val i = a.indexOf(0)
		val row = i / 3
		val col = i % 3
		def +(action: Action): State = action.dir match {
			case Direction.Up => State(swap(a, i, i - 3))
			case Direction.Dn => State(swap(a, i, i + 3))
			case Direction.Rt => State(swap(a, i, i + 1)) 
			case Direction.Lt => State(swap(a, i, i - 1))
		}
		override def toString = a.mkString("[", " ", "]")
	}
	
	object Direction extends Enumeration {
		val Up, Dn, Rt, Lt = Value 
	}
	
	case class Action(dir: Direction.Value) {
	}
	
	case class Node(
		val state: State,
		val parentOpt: Option[Node],
		val pathCost: Int
	) extends ch03.Node[State] with NodeCost[Int] with NodeHeuristic[Int] {
		val g = distance(state.a)
		val heuristic = pathCost + g
		override def getPrintString: String = {
			s"${pathCost}+$g=${heuristic}: ${state}"
		}
		override def toString = getPrintString
	}

	class EightPuzzleProblem extends Problem[State, Action, Node] {
		// Initial state for Question 1
		//val state0 = new State(Seq(1, 6, 4, 8, 7, 0, 3, 2, 5))
		// Initial state for Question 2
		val state0 = new State(Seq(8, 1, 7, 4, 5, 6, 2, 0, 3))
		
		val root = new Node(state0, None, 0)
		
		def goalTest(state: State): Boolean = state.a match {
			case Seq(0,1,2,3,4,5,6,7,8) => true
			case _ => false
		}
		
		def actions(state: State): Iterable[Action] = {
			List[Option[Action]](
				if (state.row > 0) Some(Action(Direction.Up)) else None,
				if (state.row < 2) Some(Action(Direction.Dn)) else None,
				if (state.col < 2) Some(Action(Direction.Rt)) else None,
				if (state.col > 0) Some(Action(Direction.Lt)) else None
			).flatten
		}
		
		def childNode(parent: Node, action: Action): Node = {
			val state = parent.state + action
			new Node(state, Some(parent), parent.pathCost + 1)
		}
	}
	
	class Frontier extends MinFirstFrontier[State, Int, Node] {
		def compareNodes(a: Node, b: Node): Int = b.heuristic - a.heuristic
	}
	
	def run() {
		val problem = new EightPuzzleProblem
		val search = new HeuristicGraphSearch[State, Action, Int, Node]
		val debug = new DebugSpec(printExpanded = true)
		search.run(problem, new Frontier, debug) match {
			case None => println("Not found")
			case Some(node) => node.printChain
		}
	}
}
