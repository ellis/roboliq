package ailib.ch03.hw

import ailib.ch03
import ailib.ch03._


object Missionaries {
	case class State(boat: Int, a: Int, b: Int) {
		assert(a >= 0 && a <= 3)
		assert(b >= 0 && b <= 3)
		assert(boat == -1 || boat == 1)
	
		def isValid: Boolean =
			(a == 0 || a == 3 || a == b)
		def onBoatSide: Tuple2[Int, Int] =
			if (boat == 1) (a, b)
			else (3 - a, 3 - b)
		
		def +(action: Action): State = State(boat * -1, a + action.da, b + action.db)
	}
	
	case class Action(da: Int, db: Int)
	
	case class Node(
		val state: State,
		val parentOpt: Option[Node],
		val pathCost: Int
	) extends ch03.Node[State] with NodeCost[Int] {
		override def getPrintString: String = {
			pathCost.toString + ": " + state.toString
		}
	}

	class MissionariesProblem extends Problem[State, Action, Node] {
		val state0 = new State(1, 3, 3)
		
		val root = new Node(state0, None, 0)
		
		def goalTest(state: State): Boolean = state match {
			case State(-1, 0, 0) => true
			case _ => false
		}
		
		def actions(state: State): Iterable[Action] = {
			val factor = -state.boat
			val (aMax, bMax) = state.onBoatSide
			//println(aMax, bMax)
			for {
				a <- 0 to aMax
				b <- 0 to bMax
				n = a + b
				if n > 0 && n <= 2
				da = a * factor
				db = b * factor
				action = Action(da, db)
				state2 = state + action
				//dummy = println("next: "+next)
				if state2.isValid
			} yield {
				action
			}
		}
		
		def childNode(parent: Node, action: Action): Node = {
			val state = parent.state + action
			new Node(state, Some(parent), parent.pathCost + 1)
		}
	}
	
	def run() {
		val problem = new MissionariesProblem
		val treeSearch = new TreeSearch[State, Action, Node]
		val debug = new DebugSpec()//printExpanded = true)
		treeSearch.run(problem, new BreadthFirstFrontier, debug) match {
			case None => println("Not found")
			case Some(node) => node.printChain
		}
	}
}
