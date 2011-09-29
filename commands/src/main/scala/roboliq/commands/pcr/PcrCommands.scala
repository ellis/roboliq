package roboliq.commands.pcr

import roboliq.common._
import roboliq.commands._


object PcrClose extends L34F {
	type ProgramSetup = Unit
	type ProgramConfig = Unit
	
	def createProgramSetup: ProgramSetup = ()
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = Success(())
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: record device usage
	}
}

object PcrThermocycle extends L34F_Plate {
	type ProgramSetup = String
	type ProgramConfig = String
	
	def createProgramSetup: ProgramSetup = null
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = if (setup == null) Error("thermo program ID must be set") else Success(setup)
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: record device usage
	}
}

object PcrOpen extends L34F {
	type ProgramSetup = Unit
	type ProgramConfig = Unit
	
	def createProgramSetup: ProgramSetup = ()
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = Success(())
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: record device usage
	}
}

object PcrRun extends L34F {
	type ProgramSetup = String
	type ProgramConfig = String
	
	def createProgramSetup: ProgramSetup = ""
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = Success(setup)
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: record device usage
	}
}

trait PcrCommands extends RoboliqCommands {
	def pcrClose(): PcrClose.Setup = {
		val cmd = PcrClose.L4C()
		cmds += cmd
		cmd.setup
	}

	def pcrOpen(): PcrOpen.Setup = {
		val cmd = PcrOpen.L4C()
		cmds += cmd
		cmd.setup
	}

	def pcrRun(): PcrRun.Setup = {
		val cmd = PcrRun.L4C()
		cmds += cmd
		cmd.setup
	}
	
	def thermocycle(plate: Plate): PcrThermocycle.Setup = {
		var args = new PcrThermocycle.L4A(plate)
		val cmd = PcrThermocycle.L4C(args)
		cmds += cmd
		cmd.setup
	}
}
