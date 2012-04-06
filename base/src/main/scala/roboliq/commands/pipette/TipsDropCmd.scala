package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._

class TipsDropCmdBean extends CmdBean {
	@BeanProperty var tips: java.util.List[String] = null
	
	/*override def toString: String = {
		if (tips == null)
			"TipsDropBean()"
		else
			tips.mkString("TipsDropBean(", ", ", ")")
	}*/
}

class TipsDropCmdHandler extends CmdHandlerA[TipsDropCmdBean](isFinal = true) {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonEmpty("tips")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		Expand1Resources(cmd.tips.map(tip => NeedTip(tip)).toList)
	}

	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		val lTip = cmd.tips.toList.map(tip => ctx.ob.findTip_?(tip, ctx.node)).flatten
		val location_? = ctx.ob.findSystemString_?("tipsDropLocation", ctx.node)
		if (messages.hasErrors) {
			Expand2Errors()
		}
		else {
			Expand2Tokens(List(new TipsDropToken(lTip.map(_.index), location_?.get)), Nil)
		}
	}
}

case class TipsDropToken(
	val tips: List[Int],
	val location: String
) extends CmdToken
