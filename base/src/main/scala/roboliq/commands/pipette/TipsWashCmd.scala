package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._

class TipsWashCmdBean extends CmdBean {
	@BeanProperty var tips: java.util.List[String] = null
	@BeanProperty var washProgram: java.lang.Integer = null
	@BeanProperty var intensity: String = null

	/*override def toString: String = {
		if (tips == null)
			"TipsWashBean()"
		else
			"TipsWashBean("+tips.mkString(", ")+", "+washProgram+", "+intensity+")"
	}*/
}

class TipsWashCmdHandler extends CmdHandlerA[TipsWashCmdBean](isFinal = true) {
	def process(cmd: TipsWashCmdBean, ctx: ProcessorContext, node: CmdNodeBean) {
		node.checkPropertyNonNull_?(cmd, "intensity")
		for {
			lTipId <- if (cmd.tips != null) Some(cmd.tips.toList) else None
			lTip <- ctx.ob.findTips_?(cmd.tips.toList, node)
		} {
			// FIXME: need to intelligently choose wash program!!!
			val washProgram = if (cmd.washProgram != null) cmd.washProgram.toInt else 0
			val intensity = WashIntensity.withName(cmd.intensity)
			// Update state
			ctx.builder_? match {
				case None =>
				case Some(builder) =>
					for (tip <- lTip) {
						tip.stateWriter(builder).clean(intensity)
					}
			}
			
			// Create final tokens
			val liTip = lTip.map(_.index)
			if (!liTip.isEmpty) {
				node.tokens = List(new TipsWashToken(liTip, washProgram))
			}
		}
	}
}

case class TipsWashToken(
	val tips: List[Int],
	val washProgram: Int
) extends CmdToken
