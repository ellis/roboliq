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
		val lItem = Result.sequence(cmd.items.toList.map(_.toTokenItem(ctx.ob, ctx.node))) match {
			case Error(ls) => ls.foreach(messages.addError); return Expand2Errors()
			case Success(l) => l
		}
		if (messages.hasErrors) {
			Expand2Errors()
		}
		else {
			val events = lItem.flatMap(item => {
				val tipState = ctx.states.findTipState(item.tip.id) match {
					case Error(ls) => ls.foreach(messages.addError); return Expand2Errors()
					case Success(tipState) => tipState
				}
				val src = tipState.src_? match {
					case None => messages.addError("tip state must contain reference to source well"); return Expand2Errors()
					case Some(src) => src
				}
				TipDispenseEventBean(item.tip, item.well, item.volume, PipettePosition.getPositionFromPolicyNameHack(item.policy)) ::
				WellAddEventBean(item.well, src, item.volume) :: Nil
			})
			val (doc, docMarkdown) = SpirateTokenItem.toDispenseDocString(lItem, ctx.ob, ctx.states)
			Expand2Tokens(List(new DispenseToken(lItem.toList)), events.toList, doc, docMarkdown)
		}
	}
}

case class DispenseToken(
	val items: List[SpirateTokenItem]
) extends CmdToken
