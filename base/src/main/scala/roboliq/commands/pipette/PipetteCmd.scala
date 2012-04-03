package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._
import roboliq.commands.pipette.scheduler.PipetteScheduler
import roboliq.devices.pipette.PipetteDevice


class PipetteCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[PipetteCmdItemBean] = null
	@BeanProperty var src: String = null
	@BeanProperty var dest: String = null
	@BeanProperty var volume: java.util.List[java.math.BigDecimal] = null
	@BeanProperty var premix: MixSpecBean = null
	@BeanProperty var postmix: MixSpecBean = null
	@BeanProperty var policy: String = null
	@BeanProperty var tipModel: String = null
}

class PipetteCmdItemBean {
	@BeanProperty var src: String = null
	@BeanProperty var dest: String = null
	@BeanProperty var volume: java.util.List[java.math.BigDecimal] = null
	@BeanProperty var premix: MixSpecBean = null
	@BeanProperty var postmix: MixSpecBean = null
	//tipOverrides
	@BeanProperty var policy: String = null
	@BeanProperty var tipModel: String = null
}

class PipetteCmdHandler(device: PipetteDevice) extends CmdHandlerA[PipetteCmdBean](isFinal = false) {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[PipetteCmdItemBean] = null
	
	def check(command: CmdBean): CmdHandlerCheckResult = {
		val cmd = command.asInstanceOf[PipetteCmdBean]
		val items = if (cmd.items != null) cmd.items.toList else Nil
		new CmdHandlerCheckResult(
			lPart = cmd.src :: cmd.dest :: items.flatMap(item => List(item.src, item.dest)).toList,
			lObj = items.map(item => item.tipModel).toList,
			lPoolNew = Nil
		)
	}
	
	def handle(cmd: PipetteCmdBean, ctx: ProcessorContext, node: CmdNodeBean) {
		node.mustBeNonEmpty(cmd, "items")
		if (node.getErrorCount == 0) {
			PipetteScheduler.createL3C(cmd, ctx.ob, node) match {
				case None =>
				case Some(l3c) =>
					val scheduler = new PipetteScheduler(device, ctx, l3c)
					scheduler.translate() match {
						case Error(ls) =>
						case Success(l) =>
							node.translations = l.toList
					}
			}
		}
	}
}
