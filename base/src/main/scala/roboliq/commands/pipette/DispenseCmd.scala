package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class DispenseCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[SpirateCmdItemBean] = null
}

class DispenseCmdHandler extends CmdHandlerA[DispenseCmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonEmpty("items")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		Expand1Resources(
			cmd.items.map(item => NeedDest(item.well)).toList
		)
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
			val events = lItem.flatMap(item => {
				val tip = ctx.ob.findTip(item.tip) match {
					case Error(ls) => ls.foreach(messages.addError); return Expand2Errors()
					case Success(tip) => tip
				}
				val tipState = ctx.states.findTipState(item.tip).get
				TipDispenseEventBean(tip, item.well, item.volume, item.policy) ::
				WellAddEventBean(item.well, tipState.src_?.get, item.volume) :: Nil
			})
			Expand2Tokens(List(new DispenseToken(lItem)), events)
		}
	}
}

case class DispenseToken(
	val items: List[SpirateTokenItem]
) extends CmdToken
