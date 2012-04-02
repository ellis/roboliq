package roboliq.core

import scala.reflect.BeanProperty

class CmdHandler(val isFinal: Boolean) {
	def process(command: CmdBean, ctx: ProcessorContext): CmdNodeBean
}

class CmdHandlerA[A](isFinal: Boolean) extends CmdHandler(isFinal) {
	def process(command: CmdBean, ctx: ProcessorContext): CmdNodeBean = {
		val node = new CmdNodeBean
		node.command = command
		val cmd = command.asInstanceOf[A]
		process(cmd, ctx, node)
		node
	}
	
	def process(cmd: A, ctx: ProcessorContext, node: CmdNodeBean): Unit
}