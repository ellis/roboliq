package roboliq.commands

import roboliq.common._

trait L12F[DeviceConfig] { top =>
	type ProgramParams
	
	def updateState(builder: StateBuilder, cmd2: L2C)	
	
	
	/** Derived class needs to define updateState() method */
	case class L2C(args: L2A) extends CommandL2 {
		type L1Type = top.L1C
		
		def updateState(builder: StateBuilder) =
			top.updateState(builder, this)
		
		def toL1(states: RobotState): Result[L1Type] = {
			Success(L1C(args))
		}
		
		override def toDebugString = {
			import args._
			top.getClass().getSimpleName() + this.getClass().getSimpleName() + List(device, program).mkString("(", ", ", ")") 
		}
	}
	
	case class L12A(
		val device: DeviceConfig,
		val program: ProgramParams
	)
	type L2A = L12A
	type L1A = L12A
	
	case class L1C(args: L1A) extends CommandL1 {
		override def toDebugString = {
			import args._
			top.getClass().getSimpleName() + this.getClass().getSimpleName() + List(device, program).mkString("(", ", ", ")") 
		}
	}
}
