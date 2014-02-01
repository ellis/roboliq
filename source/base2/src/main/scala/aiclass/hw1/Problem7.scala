package aiclass.hw1

import ailib.ch03
import ailib.ch03._


class AStarQuestion {
	case class State(row: Int, col: Int) {
		def +(action: Action): State = State(row + action.drow, col + action.dcol)
		override def toString = (row + 'a').asInstanceOf[Char].toString + (col + 1)
	}

	case class Action(drow: Int, dcol: Int)
	
	case class Node(
		val state: State,
		val parentOpt: Option[Node],
		val pathCost: Int,
		val heuristic: Int
	) extends ch03.Node[State] with NodeCost[Int] with NodeHeuristic[Int] {
		override def getPrintString: String = {
			state.toString + ": g=" + pathCost.toString + ", f=" + heuristic
		}
	}
	
	class Problem extends ch03.Problem[State, Action, Node] {
		val state0 = new State(0, 0)
		
		val root = new Node(state0, None, 0, 4)
		
		def goalTest(state: State): Boolean = state match {
			case State(3, 5) => true
			case _ => false
		}
		
		def actions(state: State): Iterable[Action] = {
			val range = List((0, 1), (0, -1), (1, 0), (-1, 0))
			for {
				(drow, dcol) <- range
				row = state.row + drow
				col = state.col + dcol
				if (row >= 0 && row < 4)
				if (col >= 0 && col < 6)
			} yield {
				Action(drow, dcol)
			}
		}
		
		def childNode(parent: Node, action: Action): Node = {
			val state = parent.state + action
			val g = parent.pathCost + 1
			val h = {
				if (state.row == 3 && state.col == 5) 0
				else if (state.row == 3 || state.col == 5) 1
				else if (state.row == 2 || state.col == 4) 2
				else if (state.row == 1 || state.col == 3) 3
				else 4
			}
			val f = g + h
			new Node(state, Some(parent), g, f)
		}
	}
	
	def handle(solution: Option[Node]) {
		solution match {
			case None => println("Not found")
			case Some(node) =>
				println("chain:", node)
				node.printChain
		}
	}
}

object AStarQuestion {
	def searchGraph() {
		val question = new AStarQuestion
		val problem = new question.Problem
		val search = new GraphSearch[question.State, question.Action, question.Node]
		val frontier = new MinFirstFrontier[question.State, Int, question.Node] {
			def compareNodes(a: question.Node, b: question.Node): Int = b.heuristic - a.heuristic 
		}
		//val searchBFS = new BreadthFirstSearch[State, Action]
		//val searchDFS = new DepthFirstSearch[State, Action]
		val debug = new DebugSpec(printFrontier = false, printExpanded = true)
		println("A*:")
		question.handle(search.run(problem, frontier, debug))
	}

}

object Problem7 {
	def run() {
		AStarQuestion.searchGraph()
	}
}
