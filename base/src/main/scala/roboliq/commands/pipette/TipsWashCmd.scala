package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._

class TipsWashCmdBean extends CmdBean {
	@BeanProperty var tips: java.util.List[String] = null
	@BeanProperty var washProgram: java.lang.Integer = null
	@BeanProperty var intensity: String = null
}

class TipsWashCmdHandler extends CmdHandlerA[TipsWashCmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonNull("intensity")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		val tips = if (cmd.tips == null) Nil else cmd.tips.toList
		Expand1Resources(tips.map(tip => NeedTip(tip)).toList)
	}

	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		val lTip = cmd.tips.toList.map(tip => ctx.ob.findTip_?(tip, messages)).flatten
		if (messages.hasErrors)
			return Expand2Errors()

		
		// FIXME: need to intelligently choose wash program!!!
		val washProgram = if (cmd.washProgram != null) cmd.washProgram.toInt else 0
		val degree = WashIntensity.withName(cmd.intensity)
		
		// Create final tokens
		val tokens = List(new TipsWashToken(lTip, washProgram))
		// Events
		val events = lTip.map(tip => TipCleanEventBean(tip, degree))
		
		Expand2Tokens(tokens, events)
	}
}

case class TipsWashToken(
	val tips: List[Tip],
	val washProgram: Int
) extends CmdToken
