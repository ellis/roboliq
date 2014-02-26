package roboliq.input.commands

import roboliq.entities.WorldStateEvent
import roboliq.entities.WorldState
import roboliq.core._

trait Action {
	def effects: List[WorldStateEvent]
}

class PlanPath(val action_r: List[Action], val state: WorldState) {
	def add(action: Action): RqResult[PlanPath] = {
		for {
			state1 <- WorldStateEvent.update(action.effects, state)
		} yield {
			new PlanPath(action :: action_r, state1)
		}
	}

	def add(action_l: List[Action]): RqResult[PlanPath] = {
		action_l match {
			case Nil => RqSuccess(this)
			case action :: rest =>
				for {
					path1 <- add(action)
					path2 <- path1.add(rest)
				} yield path2
		}
	}
	
	def print {
		println("PlanPath:")
		println("actions:")
		action_r.reverse.foreach(println)
		println("state:")
		println(state)
	}
}