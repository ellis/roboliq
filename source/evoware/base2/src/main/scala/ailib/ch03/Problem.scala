package ailib.ch03


abstract class Problem[State, Action, Node] {
	val state0: State
	
	def root: Node
	
	def goalTest(state: State): Boolean
	
	def actions(state: State): Iterable[Action]
	
	def childNode(parent: Node, action: Action): Node
}
