/*package roboliq.compiler

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.robot._


abstract class Compiler {
	//var robot: Robot = _
	//var state: RobotState = _
	private var m_handlers: List[CommandHandler] = Nil
	
	def register(handler: CommandHandler) {
		m_handlers = handler :: m_handlers
	}

	def compile(cmd: Command): CompileResult = {
		
	}

		def x(handlers: List[CommandHandler]): CompileResult =  handlers match {
			case Nil =>
				new CompileError(
						cmd = cmd,
						errors = List("Unhandled command: "+cmd.getClass().getCanonicalName()))
			case handler :: rest =>
				callHandlerCompile(handler, cmd) match {
					case Some(res) => res
					case None => x(rest)
				}
		}
		x(m_handlers)
	}
	
	def compileMore(res: CompileResult): Option[CompileResult] = {
		res match {
			case CompileError(_, _) => None
			case CompilePending(cmd) => Some(compile(cmd))
			case CompileSuccess(cmd, ress) =>
				val ress_? = ress.map(compileMore)
				if (ress_?.exists(_.isDefined))
					Some(CompileSuccess(cmd, ress_?.map(_.get)))
				else
					None
		}
	}
	
	protected def callHandlerCompile(handler: CommandHandler, cmd: Command): Option[CompileResult]
}
*/