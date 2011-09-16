package roboliq.commands.pcr

import roboliq.common._
import roboliq.commands._


object PcrClose extends DeviceCommand0Family {
	type ProgramSetup = Int
	type ProgramConfig = Int
	type ProgramParams = Int
	
	def createProgramSetup: ProgramSetup = 0
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = Success(0)
	def createProgramParams: ProgramParams = 0
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: record device usage
	}
	
	def updateState(builder: StateBuilder, cmd2: L2C) {
		// TODO: record the change in device state
	}
}

object PcrOpen extends DeviceCommand0Family {
	type ProgramSetup = Int
	type ProgramConfig = Int
	type ProgramParams = Int
	
	def createProgramSetup: ProgramSetup = 0
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = Success(0)
	def createProgramParams: ProgramParams = 0
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: record device usage
	}
	
	def updateState(builder: StateBuilder, cmd2: L2C) {
		// TODO: record the change in device state
	}
}

object PcrRun extends DeviceCommand0Family {
	type ProgramSetup = Int
	type ProgramConfig = Int
	type ProgramParams = Int
	
	def createProgramSetup: ProgramSetup = 0
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = Success(0)
	def createProgramParams: ProgramParams = 0
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: record device usage
	}
	
	def updateState(builder: StateBuilder, cmd2: L2C) {
		// TODO: record the change in device state
	}
}

trait PcrCommands extends RoboliqCommands {
	def pcrClose(plate: Plate): PcrClose.Setup = {
		val cmd = PcrClose.L4C()
		cmds += cmd
		cmd.setup
	}

	def pcrOpen(plate: Plate): PcrOpen.Setup = {
		val cmd = PcrOpen.L4C()
		cmds += cmd
		cmd.setup
	}

	def pcrRun(plate: Plate): PcrRun.Setup = {
		val cmd = PcrRun.L4C()
		cmds += cmd
		cmd.setup
	}
}
