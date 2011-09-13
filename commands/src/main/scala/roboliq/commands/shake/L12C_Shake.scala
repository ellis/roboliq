/*package roboliq.commands.shake

import roboliq.common._


case class L2C_Shake(args: L2A_ShakeArgs) extends CommandL2 {
	type L1Type = L1C_Shake
	
	def updateState(builder: StateBuilder) {
		// No state
	}
	
	def toL1(states: RobotState): Result[L1Type] = {
		val args1 = args.toL1(states) match {
			case Error(lsError) => return Error(lsError)
			case Success(args1) => args1
		}
		Success(L1C_Shake(args1))
	}
	
	override def toDebugString = {
		import args._
		this.getClass().getSimpleName() + List(idDevice, nDuration).mkString("(", ", ", ")") 
	}

}

case class L2A_ShakeArgs(
	idDevice: String,
	nDuration: Double
) {
	def toL1(states: StateMap): Result[L1A_ShakeArgs] = {
		Success(L1A_ShakeArgs(
			idDevice = idDevice,
			nDuration = nDuration
		))
	}
}

case class L1C_Shake(args: L1A_ShakeArgs) extends CommandL1
case class L1A_ShakeArgs(
	idDevice: String,
	nDuration: Double
)
*/