package roboliq.entities

import roboliq.core._
import roboliq.input.Protocol
import roboliq.commands.Command

abstract class ClientScriptBuilder {
	def addCommand(
		protocol: Protocol,
		state0: WorldState,
		agentIdent: String,
		command: Command
	): RsResult[WorldState]
	
	/**
	 * Needs to be called once script building is finished. 
	 */
	def end(): RsResult[Unit]
}