package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._
import roboliq.commands.pipette.scheduler.PipetteScheduler
import roboliq.devices.pipette.PipetteDevice
import roboliq.commands.pipette.scheduler.L3C_Pipette


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
	@BeanProperty var tipReplacement: String = null
	@BeanProperty var allowMultipipette: java.lang.Boolean = null
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

/*class TipOverridesBean {
	@BeanProperty var allowMultipipette: java.lang.Boolean = false
}*/

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
		scheduler.PipettePlanner.run(
			bean = cmd,
			grouper = new method.SimpleGrouper01,
			device = device,
			ctx = ctx
		) match {
			case Error(ls) => ls.foreach(ctx.node.addError); Expand2Errors()
			case Success((cmd, cmd_l)) =>
				val (doc, docMarkDown) = L3C_Pipette(cmd).toDocString(ctx.ob, ctx.states)
				Expand2Cmds(cmd_l.toList, Nil, doc, docMarkDown)
		}
		/*
		PipetteScheduler.createL3C(cmd, ctx.states) match {
			case Error(ls) => ls.foreach(ctx.node.addError); Expand2Errors()
			case Success(l3c) =>
				val scheduler = new PipetteScheduler(device, ctx, l3c)
				scheduler.translate() match {
					case Error(ls) => Expand2Errors()
					case Success(l) =>
						val (doc, docMarkDown) = l3c.toDocString(ctx.ob, ctx.states)
						Expand2Cmds(l.toList, Nil, doc, docMarkDown)
				}
		}
		*/
	}
}
