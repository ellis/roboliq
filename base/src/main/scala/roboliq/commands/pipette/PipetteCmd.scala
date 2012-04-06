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

class PipetteCmdHandler extends CmdHandlerA[PipetteCmdBean] {
	@BeanProperty var device: PipetteDevice = null
	
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		if (cmd.items == null || cmd.items.isEmpty()) {
			messages.paramMustBeNonNull("src")
			messages.paramMustBeNonNull("dest")
			messages.paramMustBeNonNull("volume")
		}
		else {
			
		}
		if (messages.hasErrors)
			return Expand1Errors()
		
		Expand1Resources(List(
			NeedSrc(cmd.src),
			NeedDest(cmd.dest)
		))
	}

	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		PipetteScheduler.createL3C(cmd, ctx.ob, ctx.node) match {
			case None => Expand2Errors()
			case Some(l3c) =>
				val scheduler = new PipetteScheduler(device, ctx, l3c)
				scheduler.translate() match {
					case Error(ls) => Expand2Errors()
					case Success(l) =>
						Expand2Cmds(l.toList, Nil)
				}
		}
	}
}
