package roboliq.commands

import roboliq.common._

trait L34F { top =>
	type ProgramSetup
	type ProgramConfig
	type L4C <: IL4C
	type L3C <: IL3C
	
	def createProgramSetup: ProgramSetup
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig]
	def createL3C(args: L3A): L3C
	
	def addKnowledge(kb: KnowledgeBase, cmd: IL4C)/* {
		// TODO: note device usage
	}*/
	
	def toL3(states: RobotState, cmd4: IL4C): Result[L3C] = {
		for { program <- createProgramConfig(cmd4.setup.program) }
		yield createL3C(new L3A(
			cmd4.setup.device_?,
			program
		))
	}

	abstract class IL4C extends CommandL4 {
		type L3Type = top.L3C
		
		val setup = new Setup
	
		def addKnowledge(kb: KnowledgeBase) =
			top.addKnowledge(kb, this)
		
		def toL3(states: RobotState): Result[L3Type] =
			top.toL3(states, this)
	
		override def toDebugString = {
			import setup._
			top.getClass().getSimpleName() + this.getClass().getSimpleName + List(device_?, program).mkString("(", ", ", ")") 
		}
	}
	
	class Setup {
		var device_? : Option[Device] = None
		val program: ProgramSetup = createProgramSetup
	}
	
	abstract class IL3C(args: L3A) extends CommandL3 {
		override def toDebugString = {
			import args._
			top.getClass().getSimpleName() + this.getClass().getSimpleName() + List(device_?, program).mkString("(", ", ", ")") 
		}
	}
	
	case class L3A(
		val device_? : Option[Device],
		var program: ProgramConfig
	)
}
