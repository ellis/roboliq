package roboliq.entities

import roboliq.core.RsResult
import roboliq.input.Context
import roboliq.input.Instruction

/**
 * @param agentName The name of the primary agent which this builder is associated with.  Used for naming script files.
 */
abstract class ClientScriptBuilder(val agentName: String) {
	def addCommand(
		agentIdent: String,
		instruction: Instruction
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