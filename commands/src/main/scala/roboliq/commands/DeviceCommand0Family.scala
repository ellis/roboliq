package roboliq.commands

import roboliq.common._

trait DeviceCommand0Family { top =>
	type ProgramSetup
	type ProgramConfig
	type ProgramParams
	
	def createProgramSetup: ProgramSetup
	def createProgramConfig(setup: ProgramSetup): Result[ProgramConfig]
	def createProgramParams: ProgramParams
	
	def addKnowledge(kb: KnowledgeBase, cmd: L4C) {
		// TODO: note that plate will occupy the target location
		// TODO: request plate compatibility with this device
	}
	
	def toL3(states: RobotState, cmd4: L4C): Result[L3C] = {
		for { program <- createProgramConfig(cmd4.setup.program) }
		yield L3C(new L3A(
			cmd4.setup.idDevice_?,
			program
		))
	}
	
	def updateState(builder: StateBuilder, cmd2: L2C)	

	case class L4C extends CommandL4 {
		type L3Type = top.L3C
		
		val setup = new Setup
	
		def addKnowledge(kb: KnowledgeBase) =
			top.addKnowledge(kb, this)
		
		def toL3(states: RobotState): Result[L3Type] =
			top.toL3(states, this)
	
		override def toDebugString = {
			import setup._
			top.getClass().getSimpleName() + this.getClass().getSimpleName + List(idDevice_?, program).mkString("(", ", ", ")") 
		}
	}
	
	class Setup {
		var idDevice_? : Option[String] = None
		val program: ProgramSetup = createProgramSetup
	}
	
	case class L3C(args: L3A) extends CommandL3 {
		override def toDebugString = {
			import args._
			top.getClass().getSimpleName() + this.getClass().getSimpleName() + List(idDevice_?, program).mkString("(", ", ", ")") 
		}
	}
	
	class L3A(
		val idDevice_? : Option[String],
		var program: ProgramConfig
	)
	
	/** Derived class needs to define updateState() method */
	case class L2C(args: L12A) extends CommandL2 {
		type L1Type = top.L1C
		
		def updateState(builder: StateBuilder) =
			top.updateState(builder, this)
		
		def toL1(states: RobotState): Result[L1Type] = {
			Success(L1C(args))
		}
		
		override def toDebugString = {
			import args._
			top.getClass().getSimpleName() + this.getClass().getSimpleName() + List(idDevice, program).mkString("(", ", ", ")") 
		}
	}
	
	case class L12A(
		val idDevice: String,
		val program: ProgramParams
	)
	
	case class L1C(args: L12A) extends CommandL1 {
		override def toDebugString = {
			import args._
			top.getClass().getSimpleName() + this.getClass().getSimpleName() + List(idDevice, program).mkString("(", ", ", ")") 
		}
	}
}
