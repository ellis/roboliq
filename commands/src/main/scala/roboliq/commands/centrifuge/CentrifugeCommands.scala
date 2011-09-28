package roboliq.commands.centrifuge

import roboliq.common._
import roboliq.commands._


object CentrifugeClose extends L34F {
	type ProgramSetup = Unit
	type ProgramConfig = Unit
	
	def createProgramSetup: ProgramSetup = ()
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = Success(())
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: record device usage
	}
}

object CentrifugeMoveTo extends L34F {
	type ProgramSetup = Int
	type ProgramConfig = Int
	
	def createProgramSetup: ProgramSetup = -1
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = 
		for { _ <- Result.assert(setup > -1, "must set centrifuge position to move to") }
		yield { setup }
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: record device usage
	}
}

object CentrifugeOpen extends L34F {
	type ProgramSetup = Unit
	type ProgramConfig = Unit
	
	def createProgramSetup: ProgramSetup = ()
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = Success(())
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: record device usage
	}
}

object CentrifugeRun extends L34F {
	type ProgramSetup = String
	type ProgramConfig = String
	
	def createProgramSetup: ProgramSetup = ""
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = Success(setup)
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: record device usage
	}
}

trait CentrifugeCommands extends RoboliqCommands {
	def centrifugeClose(): CentrifugeClose.Setup = {
		val cmd = CentrifugeClose.L4C()
		cmds += cmd
		cmd.setup
	}

	def centrifugeOpen(): CentrifugeOpen.Setup = {
		val cmd = CentrifugeOpen.L4C()
		cmds += cmd
		cmd.setup
	}

	def centrifugeRun(): CentrifugeRun.Setup = {
		val cmd = CentrifugeRun.L4C()
		cmds += cmd
		cmd.setup
	}

	def centrifuge(plate: Plate): L4A_CentrifugeSetup = {
		val args = new L4A_CentrifugeArgs(Seq(plate))
		val cmd = L4C_Centrifuge(args)
		cmds += cmd
		cmd.setup
	}
}
