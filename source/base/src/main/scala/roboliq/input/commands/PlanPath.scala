package roboliq.input.commands

import roboliq.entities.WorldStateEvent
import roboliq.entities.WorldState
import roboliq.core._
import roboliq.input.Context

trait Action {
	def effects: List[WorldStateEvent]
	
	def updateState: Context[Unit] = {
		for {
			state0 <- Context.gets(_.state)
			_ <- Context.from(WorldStateEvent.update(effects, state0))
		} yield ()
	}
}

// REFACTOR: Remove class PlanPath
class PlanPath(val action_r: List[Action], val state: WorldState) {
	def add(action: Action): RqResult[PlanPath] = {
		//println("add")
		//println("add: "+action)
		for {
			state1 <- WorldStateEvent.update(action.effects, state)
		} yield {
			val path1 = new PlanPath(action :: action_r, state1)
			//println("added:")
			//println(" " + path1.action_r.reverse.map(action => action.getClass().getSimpleName()).mkString(", "))
			//path1.state.well_aliquot_m.filter(_._1.label.get.contains("A01")).foreach(pair => println(s"${pair._1.label.get}: ${pair._2}"))
			path1
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