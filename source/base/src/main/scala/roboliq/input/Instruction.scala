package roboliq.input

import roboliq.entities.WorldStateEvent
import roboliq.entities.Agent

trait Instruction {
	def effects: List[WorldStateEvent]
	
	def updateState: Context[Unit] = {
		for {
			state0 <- Context.gets(_.state)
			state1 <- Context.from(WorldStateEvent.update(effects, state0))
			_ <- Context.modify(_.copy(state = state1))
		} yield ()
	}
}

case class AgentInstruction(
	val agent: Agent,
	val instruction: Instruction
)