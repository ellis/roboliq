package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class AspirateCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[SpirateCmdItemBean] = null
}

class AspirateCmdHandler extends CmdHandlerA[AspirateCmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonEmpty("items")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		Expand1Resources(
			cmd.items.map(item => NeedSrc(item.well)).toList
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
				TipAspirateEventBean(item.tip, item.well, item.volume) ::
				WellRemoveEventBean(item.well, item.volume) :: Nil
			})
			Expand2Tokens(List(new AspirateToken(lItem.toList)), events.toList, null)
		}
	}
}

class SpirateCmdItemBean {
	@BeanProperty var tip: String = null
	@BeanProperty var well: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var policy: String = null
	@BeanProperty var mixSpec: MixSpecBean = null
	
	def toTokenItem(ob: ObjBase, node: CmdNodeBean): Result[SpirateTokenItem] = {
		node.checkPropertyNonNull_?(this, "tip", "well", "volume", "policy")
		if (node.getErrorCount != 0)
			return Error(Nil)
		for {
			tipObj <- ob.findTip(tip)
			wellObj <- ob.findWell2(well)
		} yield {
			new SpirateTokenItem(tipObj, wellObj, LiquidVolume.l(volume), policy)
		}
	}
}

case class AspirateToken(
	val items: List[SpirateTokenItem]
) extends CmdToken

case class SpirateTokenItem(
	val tip: Tip,
	val well: Well2,
	val volume: LiquidVolume,
	val policy: String
) extends HasTipWell
