package ailib.ch03


trait Node[State] {
	val state: State
	val parentOpt: Option[Node[State]]

	def getPrintString: String = {
		state.toString
	}
	
	def printChain() {
		val l = makeChain(Nil)
		l.foreach(n => println(n.getPrintString))
	}
	
	private def makeChain(l: List[Node[State]]): List[Node[State]] = parentOpt match {
		case None => this :: l
		case Some(parent) => parent.makeChain(this :: l)
	}
}

trait NodeCost[T] {
	val pathCost: T
}

trait NodeAction[Action] {
	val action: Action
}

trait NodeHeuristic[T] {
	val heuristic: T
}
