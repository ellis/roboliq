package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class DispenseCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[SpirateCmdItemBean] = null
}

class DispenseCmdHandler extends CmdHandlerA[DispenseCmdBean](isFinal = true) {
	def process(cmd: AspirateCmdBean, ctx: ProcessorContext, node: CmdNodeBean) {
		node.mustBeNonEmpty(cmd, "items")
		if (node.getErrorCount == 0) {
			val lItem = cmd.items.toList.map(_.toTokenItem(ctx.ob, node)).flatten
			if (node.getErrorCount == 0) {
				node.tokens = List(new DispenseToken(lItem))
			}
		}
	}
}

case class DispenseToken(
	val items: List[SpirateTokenItem]
) extends CmdToken
