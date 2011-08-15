/*package roboliq.compiler


abstract class L1_CommandHandler extends CommandHandler {
	def compile1(cmd: L1_Command): Option[CompileResult]
}

class L1_Compiler extends Compiler {
	override protected def callHandlerCompile(handler: CommandHandler, cmd: Command): Option[CompileResult] = {
		if (handler.isInstanceOf[L1_CommandHandler] && cmd.isInstanceOf[L1_Command]) {
			val handler1 = handler.asInstanceOf[L1_CommandHandler]
			val cmd1 = cmd.asInstanceOf[L1_Command]
			handler1.compile1(cmd1)
		}
		else
			None
	}
}
*/