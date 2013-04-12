/*package roboliq.level2

import roboliq.compiler._
import roboliq.robot._


abstract class L3_CommandHandler extends CommandHandler {
	def compile2(robot: Robot, state0: RobotState, cmd: L3_Command): Option[CompileResult]
}

class L3_Compiler extends Compiler {
	override protected def callHandlerCompile(handler: CommandHandler, cmd: Command): Option[CompileResult] = {
		if (handler.isInstanceOf[L3_CommandHandler] && cmd.isInstanceOf[L3_Command]) {
			val handler2 = handler.asInstanceOf[L3_CommandHandler]
			val cmd2 = cmd.asInstanceOf[L3_Command]
			handler2.compile2(cmd2)
		}
		else
			None
	}
}
*/