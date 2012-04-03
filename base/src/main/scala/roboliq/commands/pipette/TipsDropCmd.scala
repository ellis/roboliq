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
	def check(command: CmdBean): CmdHandlerCheckResult = {
		val cmd = command.asInstanceOf[TipsDropCmdBean]
		val tips = if (cmd.tips != null) cmd.tips.toList else Nil
		new CmdHandlerCheckResult(
			lPart = Nil,
			lObj = tips,
			lPoolNew = Nil
		)
	}
	
	def handle(cmd: TipsDropCmdBean, ctx: ProcessorContext, node: CmdNodeBean) {
		for {
			lId <- if (cmd.tips != null) Some(cmd.tips.toList) else None
			lTip <- ctx.ob.findTips_?(cmd.tips.toList, node)
			location <- ctx.ob.findSystemString_?("tipsDropLocation", node)
		} {
			// Update state
			ctx.builder_? match {
				case None =>
				case Some(builder) =>
					for (tip <- lTip) {
						tip.stateWriter(builder).drop()
					}
			}
			
			// Create final tokens
			val liTip = lTip.map(_.index)
			if (!liTip.isEmpty) {
				node.tokens = List(new TipsDropToken(liTip, location))
			}
		}
	}
}

case class TipsDropToken(
	val tips: List[Int],
	val location: String
) extends CmdToken
