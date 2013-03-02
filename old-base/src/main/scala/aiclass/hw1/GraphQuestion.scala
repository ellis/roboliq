package aiclass.hw1

import ailib.ch03
import ailib.ch03._


class GraphQuestion {
	case class State(id: Int) {
		def +(action: Action): State = State(action.id)
	}

	case class Action(id: Int)
	
	case class Node(
		val state: State,
		val parentOpt: Option[Node],
		val pathCost: Int
	) extends ch03.Node[State] with NodeCost[Int] {
		override def getPrintString: String = {
			pathCost.toString + ": " + state.toString
		}
	}
	
	class Problem(mapChildren: Map[Int, List[Int]], idGoal: Int) extends ch03.Problem[State, Action, Node] {
		val state0 = new State(0)
		
		val root = new Node(state0, None, 0)
		
		def goalTest(state: State): Boolean = state.id == idGoal
		
		def actions(state: State): Iterable[Action] = {
			mapChildren.getOrElse(state.id, Nil).map(Action)
		}
		
		def childNode(parent: Node, action: Action): Node = {
			val state = parent.state + action
			new Node(state, Some(parent), parent.pathCost + 1)
		}
	}
	
	def handle(solution: Option[Node]) {
		solution match {
			case None => println("Not found")
			case Some(node) => node.printChain
		}
	}
}

object GraphQuestion {
	def searchTree(mapChildrenL: Map[Int, List[Int]], idGoal: Int) {
		val mapChildrenR = mapChildrenL.map(pair => pair._1 -> pair._2.reverse)
		
		val question = new GraphQuestion
		val problemL = new question.Problem(mapChildrenL, idGoal)
		val problemR = new question.Problem(mapChildrenR, idGoal)
		val treeSearch = new TreeSearch[question.State, question.Action, question.Node]
		//val searchBFS = new BreadthFirstSearch[State, Action]
		//val searchDFS = new DepthFirstSearch[State, Action]
		val debug = new DebugSpec(printExpanded = true)
		println("L to R: BFS")
		question.handle(treeSearch.run(problemL, new BreadthFirstFrontier, debug))
		println()
		println("L to R: DFS")
		question.handle(treeSearch.run(problemR, new DepthFirstFrontier, debug))
		println()
		println("R to L: BFS")
		question.handle(treeSearch.run(problemR, new BreadthFirstFrontier, debug))
		println()
		println("R to L: DFS")
		question.handle(treeSearch.run(problemL, new DepthFirstFrontier, debug))
	}

	def searchGraph(mapChildrenL: Map[Int, List[Int]], idGoal: Int) {
		val mapChildrenR = mapChildrenL.map(pair => pair._1 -> pair._2.reverse)
		
		val question = new GraphQuestion
		val problemL = new question.Problem(mapChildrenL, idGoal)
		val problemR = new question.Problem(mapChildrenR, idGoal)
		val search = new GraphSearch[question.State, question.Action, question.Node]
		//val searchBFS = new BreadthFirstSearch[State, Action]
		//val searchDFS = new DepthFirstSearch[State, Action]
		val debug = new DebugSpec(printExpanded = true)
		println("L to R: BFS")
		question.handle(search.run(problemL, new BreadthFirstFrontier, debug))
		println()
		println("L to R: DFS")
		question.handle(search.run(problemR, new DepthFirstFrontier, debug))
		println()
		println("R to L: BFS")
		question.handle(search.run(problemR, new BreadthFirstFrontier, debug))
		println()
		println("R to L: DFS")
		question.handle(search.run(problemL, new DepthFirstFrontier, debug))
	}

}
