package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class DispenseCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[SpirateCmdItemBean] = null
}

class DispenseCmdHandler extends CmdHandlerA[DispenseCmdBean](isFinal = true) {
	def expand1A(
		cmd: CmdType,
		messages: CmdMessageWriter
	): Option[
		Either[List[NeedResource], List[CmdBean]]
	] = {
		messages.paramMustBeNonEmpty("items")
		if (messages.hasErrors)
			return None
		
		// Item wells are sources
		Some(Left(cmd.items.flatMap(item => {
			List(NeedDest(item.well))
		}).toList))
	}

	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		val lItem = cmd.items.toList.map(_.toTokenItem(ctx.ob, ctx.node)).flatten
		if (messages.hasErrors) {
			Expand2Errors()
		}
		else {
			Expand2Tokens(List(new DispenseToken(lItem)), Nil)
		}
	}
}

case class DispenseToken(
	val items: List[SpirateTokenItem]
) extends CmdToken
