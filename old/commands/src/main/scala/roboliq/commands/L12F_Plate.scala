/*package roboliq.commands

import roboliq.common._

trait L12F_Plate[DeviceConfig] { top =>
	type ProgramParams
	
	def updateState(builder: StateBuilder, cmd2: L2C)	
	
	
	/** Derived class needs to define updateState() method */
	case class L2C(args: L2A) extends CommandL2 {
		type L1Type = top.L1C
		
		def updateState(builder: StateBuilder) =
			top.updateState(builder, this)
		
		def toL1(states: RobotState): Result[L1Type] = {
			Success(L1C(new L1A(args.device, args.program, args.plate, args.plate.state(states).location)))
		}
		
		override def toDebugString = {
			import args._
			top.getClass().getSimpleName() + this.getClass().getSimpleName() + List(device, program).mkString("(", ", ", ")") 
		}
	}
	
	case class L2A(
		val device: DeviceConfig,
		val program: ProgramParams,
		val plate: Plate
	)
	
	case class L1C(args: L1A) extends CommandL1 {
		override def toDebugString = {
			import args._
			top.getClass().getSimpleName() + this.getClass().getSimpleName() + List(device, program).mkString("(", ", ", ")") 
		}
	}

	case class L1A(
		val device: DeviceConfig,
		val program: ProgramParams,
		val plate: Plate,
		val location: String
	)
}
*/