/*package roboliq.level2

import roboliq.compiler._
import roboliq.robot._


abstract class L2_CommandHandler extends CommandHandler {
	def compile2(robot: Robot, state0: RobotState, cmd: L2_Command): Option[CompileResult]
}

class L2_Compiler extends Compiler {
	override protected def callHandlerCompile(handler: CommandHandler, cmd: Command): Option[CompileResult] = {
		if (handler.isInstanceOf[L2_CommandHandler] && cmd.isInstanceOf[L2_Command]) {
			val handler2 = handler.asInstanceOf[L2_CommandHandler]
			val cmd2 = cmd.asInstanceOf[L2_Command]
			handler2.compile2(cmd2)
		}
		else
			None
	}
}
*/