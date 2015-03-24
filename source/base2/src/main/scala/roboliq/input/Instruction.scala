package roboliq.input

import roboliq.entities.WorldStateEvent
import roboliq.entities.Agent
import roboliq.entities.WorldState
import roboliq.entities.Device

trait Instruction {
	def effects: List[WorldStateEvent]
	
	def data: List[Object]
}

/**
 * An instruction that should be routed to a DeviceInstructionHandler
 */
trait DeviceInstruction extends Instruction {
	val device: Device
}

case class AgentInstruction(
	val agent: Agent,
	val instruction: Instruction
)