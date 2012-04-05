package roboliq.core

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

abstract class CmdHandler(val isFinal: Boolean) {
	/** Return true if this handler wants to process this given command */
	def canHandle(command: CmdBean): Boolean
	def expandWithoutObjBase(command: CmdBean, index: List[Int]): Option[List[CmdBean]] = None
	def getResources(command: CmdBean): List[NeedResource]
	def handle(command: CmdBean, ctx: ProcessorContext): CmdNodeBean
}

abstract class CmdHandlerA[A <: CmdBean : Manifest](isFinal: Boolean) extends CmdHandler(isFinal) {
	def canHandle(command: CmdBean): Boolean = {
		command.isInstanceOf[A]
	}
	
	def handle(command: CmdBean, ctx: ProcessorContext): CmdNodeBean = {
		// Create node
		val node = new CmdNodeBean
		node.command = command

		// Call command's handler
		val cmd = command.asInstanceOf[A]
		handle(cmd, ctx, node)
		
		// If the handler didn't set the final state explicitly,
		if (node.states1 == null) {
			node.states1 = ctx.builder_? match {
				// If there's not builder, take the original state as the final state
				case None => ctx.states
				// Otherwise iterate through the events and return the final builder state
				case Some(builder) =>
					if (node.events != null) {
						node.events.foreach(_.update(builder)) 
					}
					builder.toImmutable
			}
		}
		
		node
	}
	
	def handle(cmd: A, ctx: ProcessorContext, node: CmdNodeBean): Unit
}