package roboliq.entities

import roboliq.core._
import roboliq.input.Protocol

abstract class ClientScriptBuilder {
	def addOperation(
		protocol: Protocol,
		state0: WorldState,
		operation: String,
		agentIdent: String,
		args: List[String]
	): RsResult[WorldState]
	
	/**
	 * Needs to be called once script building is finished. 
	 */
	def end(): RsResult[Unit]
}