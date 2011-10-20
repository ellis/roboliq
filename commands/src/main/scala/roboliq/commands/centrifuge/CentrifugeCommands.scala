package roboliq.commands.centrifuge

import roboliq.common._
import roboliq.commands._


object CentrifugeClose extends L34F {
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

object CentrifugeMoveTo extends L34F {
	type ProgramSetup = Int
	type ProgramConfig = Int
	case class L4C() extends IL4C
	case class L3C(args: L3A) extends IL3C(args)
	
	def createProgramSetup: ProgramSetup = -1
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] = 
		for { _ <- Result.assert(setup > -1, "must set centrifuge position to move to") }
		yield { setup }
	def createL3C(args: L3A): L3C = L3C(args)
	
	def addKnowledge(kb: KnowledgeBase, cmd: IL4C) {
		// TODO: record device usage
	}
}

object CentrifugeOpen extends L34F {
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

object CentrifugeRun extends L34F {
	class ProgramSetup {
		var program: String = null
	}
	type ProgramConfig = String
	case class L4C() extends IL4C
	case class L3C(args: L3A) extends IL3C(args)
	
	def createProgramSetup: ProgramSetup = new ProgramSetup
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig] =
		for { _ <- Result.assert(setup.program != null, "must set centrifuge program") }
		yield { setup.program }
	def createL3C(args: L3A): L3C = L3C(args)
	
	def addKnowledge(kb: KnowledgeBase, cmd: IL4C) {
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

	def centrifuge(plates: Plate*): L4A_CentrifugeSetup = {
		val args = new L4A_CentrifugeArgs(plates)
		val cmd = L4C_Centrifuge(args)
		cmds += cmd
		cmd.setup
	}
}
