package roboliq.core

import scala.collection.JavaConversions._

class Processor private (bb: BeanBase, ob: ObjBase, lCmdHandler: List[CmdHandler], states0: RobotState) {
	def process(cmds: List[CmdBean]): List[CmdNodeBean] = {
		val builder = new StateBuilder(states0)
		val nodes = cmds.map(cmd => {
			lCmdHandler.find(_.canHandle(cmd)) match {
				case None =>
					val node = new CmdNodeBean
					node.command = cmd
					node.errors = List("no command handler found for "+cmd.getClass().getName())
					node
				case Some(handler) =>
					val ctx = new ProcessorContext(this, ob, Some(builder), builder.toImmutable)
					handler.handle(cmd, ctx)
			}
		})
		Nil
	}
}

object Processor {
	def apply(bb: BeanBase, states0: StateMap): Processor = {
		val ob = new ObjBase(bb)
		new Processor(bb, ob, bb.lCmdHandler)
	}
}

class ProcessorContext(
	val processor: Processor,
	val ob: ObjBase,
	val builder_? : Option[StateBuilder],
	val states: RobotState
)
