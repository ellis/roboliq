package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class MixCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[MixCmdItemBean] = null
	@BeanProperty var mixSpec: MixSpecBean = null
}

class MixCmdHandler extends CmdHandlerA[MixCmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonEmpty("items")
		if (cmd.items != null) {
			cmd.items.zipWithIndex.foreach(pair => {
				val (item, i) = pair
				messages.paramMustBeNonNull("items["+(i+1)+"].tip", item.tip)
				messages.paramMustBeNonNull("items["+(i+1)+"].well", item.well)
			})
			
		}
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		Expand1Resources(
			cmd.items.flatMap(item => {
				List(NeedTip(item.tip), NeedSrc(item.well))
			}).toList
		)
	}

	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		val lItem = cmd.items.toList.map(_.toTokenItem(cmd, ctx.ob, ctx.node)).flatten
		if (messages.hasErrors) {
			Expand2Errors()
		}
		else {
			Expand2Tokens(List(new MixToken(lItem)), Nil, null)
		}
	}
}

class MixCmdItemBean {
	@BeanProperty var tip: String = null
	@BeanProperty var well: String = null
	@BeanProperty var mixSpec: MixSpecBean = null
	
	def toTokenItem(cmd: MixCmdBean, ob: ObjBase, node: CmdNodeBean): Option[MixTokenItem] = {
		val volume0 = {
			if (mixSpec != null && mixSpec.volume != null) mixSpec.volume
			else if (cmd.mixSpec != null && cmd.mixSpec.volume != null) cmd.mixSpec.volume
			else null
		}
		val count0 = {
			if (mixSpec != null && mixSpec.count != null) mixSpec.count
			else if (cmd.mixSpec != null && cmd.mixSpec.count != null) cmd.mixSpec.count
			else null
		}
		val policy0 = {
			if (mixSpec != null && mixSpec.policy != null) mixSpec.policy
			else if (cmd.mixSpec != null && cmd.mixSpec.policy != null) cmd.mixSpec.policy
			else null
		}
		for {
			idTip <- node.getValueNonNull_?(tip, "tip")
			idWell <- node.getValueNonNull_?(well, "well")
			volume <- node.getValueNonNull_?(volume0, "volume")
			count <- node.getValueNonNull_?(count0, "count")
			idPolicy <- node.getValueNonNull_?(policy0, "policy")
			tipObj <- ob.findTip_?(tip, new CmdMessageWriter(node))
			well <- ob.findWell_?(idWell, node)
		} yield {
			val policy = PipettePolicy.fromName(idPolicy)
			new MixTokenItem(tipObj, well, LiquidVolume.l(volume), count, policy)
		}
	}
}

case class MixToken(
	val items: List[MixTokenItem]
) extends CmdToken

case class MixTokenItem(
	val tip: Tip,
	val well: Well,
	val volume: LiquidVolume,
	val count: Int,
	val policy: PipettePolicy
) extends HasTip with HasWell with HasVolume with HasPolicy
