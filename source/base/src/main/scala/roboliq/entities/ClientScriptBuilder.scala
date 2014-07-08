package roboliq.entities

import roboliq.core._
import roboliq.input.Protocol
import roboliq.input.commands.Command
import roboliq.input.Context

/**
 * @param agentName The name of the primary agent which this builder is associated with.  Used for naming script files.
 */
abstract class ClientScriptBuilder(val agentName: String) {
	def addCommand(
		agentIdent: String,
		command: Command
	): Context[Unit]
	
	/**
	 * Needs to be called once script building is finished. 
	 */
	def end(): RsResult[Unit]

	/**
	 * Save script files which were built
	 */
	def saveScripts(basename: String): RsResult[Unit]
}