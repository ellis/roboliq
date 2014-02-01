package roboliq.entities

import roboliq.core._
import roboliq.input.Protocol
import roboliq.commands.Command

/**
 * @param agentName The name of the primary agent which this builder is associated with.  Used for naming script files.
 */
abstract class ClientScriptBuilder(val agentName: String) {
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

	/**
	 * Save script files which were built
	 */
	def saveScripts(basename: String): RsResult[Unit]
}