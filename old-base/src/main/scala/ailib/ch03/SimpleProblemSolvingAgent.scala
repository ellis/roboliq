package ailib.ch03

import scala.collection.mutable.ArrayBuffer


trait SimpleProblemSolvingAgent {
	type State
	type Percept
	type Action
	type Goal
	type Problem
	
	def updateState(state: State, percept: Percept): State
	
	def formulateGoal(state: State): Goal
	
	def formulateProblem(state: State, goal: Goal): Problem
	
	def search(problem: Problem): Option[Seq[Action]]
}

abstract class SimpleProblemSolvingAgentVar[TState, TPercept, TAction, TGoal, TProblem] extends SimpleProblemSolvingAgent {
	type State = TState
	type Percept = TPercept
	type Action = TAction
	type Goal = TGoal
	type Problem = TProblem
	
	var seq = Seq[Action]()
	var state: State
	
	def run(percept: Percept): Option[Action] = {
		state = updateState(state, percept)
		if (seq.isEmpty) {
			val goal = formulateGoal(state)
			val problem = formulateProblem(state, goal)
			search(problem) match {
				case None => return None
				case Some(seq2) => seq = seq2
			}
		}
		val action = seq.head
		seq = seq.tail
		return Some(action)
	}
}
	
/*
class SimpleProblemSolvingAgentStep[State, Percept, Action, Goal, Problem] extends SimpleProblemSolvingAgent {
	def step(state0: State, percept: Percept): Option[SimpleProblemSolvingAgentStep] = {
		val seq = new ArrayBuffer[Action]
		var state = state0
		
		state = updateState(state, percept)
		if (seq.isEmpty) {
			val goal = formulateGoal(state)
			val problem = formulateProblem(state, goal)
			search(problem) match {
				case None => return None
				case Some(seq2) => seq ++= seq2
			}
		}
	}
	
	def updateState(state: State, percept: Percept): State
	
	def formulateGoal(state: State)
	
	def formulateProblem(state: State, goal: Goal)
	
	def search(problem: Problem): Option[Seq[Action]]
}
*/

