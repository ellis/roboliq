package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class AspirateCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[SpirateCmdItemBean] = null
}

class AspirateCmdHandler extends CmdHandlerA[AspirateCmdBean](isFinal = true) {
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
		val lItem = cmd.items.toList.map(_.toTokenItem(ctx.ob, ctx.node)).flatten
		if (messages.hasErrors) {
			Expand2Errors()
		}
		else {
			Expand2Tokens(List(new AspirateToken(lItem)), Nil)
		}
	}
}

class SpirateCmdItemBean {
	@BeanProperty var tip: String = null
	@BeanProperty var well: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var policy: String = null
	
	def toTokenItem(ob: ObjBase, node: CmdNodeBean): Option[SpirateTokenItem] = {
		for {
			_ <- node.checkPropertyNonNull_?(this, "tip", "well", "volume", "policy")
			wellObj <- ob.findWell_?(well, node)
		} yield {
			val iTip = tip.drop(3).toInt
			new SpirateTokenItem(iTip, wellObj, LiquidVolume.l(volume), policy)
		}
	}
}

case class AspirateToken(
	val items: List[SpirateTokenItem]
) extends CmdToken

case class SpirateTokenItem(
	val tip: java.lang.Integer,
	val well: Well,
	val volume: LiquidVolume,
	val policy: String
)
