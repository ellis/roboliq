package roboliq.commands.pcr

import roboliq.common._
import roboliq.commands._


object PcrClose extends L34F {
	type ProgramSetup = Unit
	type ProgramConfig = Unit
	case class L4C() extends IL4C
	case class L3C(args: L3A) extends IL3C(args)
	
	def createProgramSetup: ProgramSetup = ()
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = Success(())
	def createL3C(args: L3A): L3C = L3C(args)

	def addKnowledge(kb: KnowledgeBase, cmd: IL4C) {
		// TODO: record device usage
	}
}

object PcrThermocycle extends L34F_Plate {
	type ProgramSetup = ThermocycleSetup
	type ProgramConfig = String
	case class L4C(_args: L4A) extends IL4C(_args)
	case class L3C(args: L3A) extends IL3C(args)
	
	def createProgramSetup: ProgramSetup = new ProgramSetup
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = if (setup.program == null) Error("thermo program ID must be set") else Success(setup.program)
	def createL3C(args: L3A): L3C = L3C(args)
	
	def addKnowledge(kb: KnowledgeBase, cmd: IL4C) {
		// TODO: record device usage
	}
	
	class ThermocycleSetup {
		var program: String = null
	}
}

object PcrOpen extends L34F {
	type ProgramSetup = Unit
	type ProgramConfig = Unit
	case class L4C() extends IL4C
	case class L3C(args: L3A) extends IL3C(args)
	
	def createProgramSetup: ProgramSetup = ()
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = Success(())
	def createL3C(args: L3A): L3C = L3C(args)
	
	def addKnowledge(kb: KnowledgeBase, cmd: IL4C) {
		// TODO: record device usage
	}
}

object PcrRun extends L34F {
	type ProgramSetup = RunSetup
	type ProgramConfig = String
	case class L4C() extends IL4C
	case class L3C(args: L3A) extends IL3C(args)
	
	def createProgramSetup: ProgramSetup = new ProgramSetup
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = if (setup.program == null) Error("thermo program ID must be set") else Success(setup.program)
	def createL3C(args: L3A): L3C = L3C(args)
	
	def addKnowledge(kb: KnowledgeBase, cmd: IL4C) {
		// TODO: record device usage
	}
	
	class RunSetup {
		var program: String = null
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
		val args = new PcrThermocycle.L4A(plate)
		val cmd = PcrThermocycle.L4C(args)
		cmds += cmd
		cmd.setup
	}
	
	def pcrMix(dest: WellPointer, items: Seq[MixItemL4], water: WellPointer, v1: Double, well_masterMix: WellPointer = null): L4A_PcrMixSetup = {
		val args = new L4A_PcrMixArgs(
			items,
			dest,
			water,
			v1
		)
		val cmd = L4C_PcrMix(args)
		cmd.setup.well_masterMix = well_masterMix
		cmds += cmd
		cmd.setup
	}
}
