package roboliq.input

import roboliq.entities.WorldStateEvent
import roboliq.entities.Agent
import roboliq.entities.WorldState

trait Instruction {
	def effects: List[WorldStateEvent]
	
	def data: List[Object]
}

case class AgentInstruction(
	val agent: Agent,
	val instruction: Instruction
)