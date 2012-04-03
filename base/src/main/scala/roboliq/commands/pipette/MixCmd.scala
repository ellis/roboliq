package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class MixCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[MixCmdItemBean] = null
	@BeanProperty var mixSpec: MixSpecBean = null
}

class MixCmdHandler extends CmdHandlerA[MixCmdBean](isFinal = true) {
	def check(command: CmdBean): CmdHandlerCheckResult = {
		val cmd = command.asInstanceOf[MixCmdBean]
		val items = if (cmd.items != null) cmd.items.toList else Nil
		new CmdHandlerCheckResult(
			lPart = items.map(item => item.well).toList,
			lObj = items.map(item => item.tip).toList,
			lPoolNew = Nil
		)
	}
	
	def handle(cmd: MixCmdBean, ctx: ProcessorContext, node: CmdNodeBean) {
		node.mustBeNonEmpty(cmd, "items")
		if (node.getErrorCount == 0) {
			val lItem = cmd.items.toList.map(_.toTokenItem(cmd, ctx.ob, node)).flatten
			if (node.getErrorCount == 0) {
				node.tokens = List(new MixToken(lItem))
			}
		}
	}
	/*
	override def toDebugString = {
		val wells = items.map(_.well)
		val sTips = TipSet.toDebugString(items.map(_.tip))
		val sVolumes = Printer.getSeqDebugString(items.map(_.getVolume()))
		val sPolicies = Printer.getSeqDebugString(items.map(_.getPolicy()))
		getClass().getSimpleName() + "("+sTips+", "+sVolumes+", "+sPolicies+", "+getWellsDebugString(wells)+")" 
	}
	*/
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
			well <- ob.findWell_?(idWell, node)
		} yield {
			val iTip = idTip.drop(3).toInt
			new MixTokenItem(iTip, well, LiquidVolume.l(volume), count, idPolicy)
		}
	}
}

case class MixToken(
	val items: List[MixTokenItem]
) extends CmdToken

case class MixTokenItem(
	val tip: java.lang.Integer,
	val well: Well,
	val volume: LiquidVolume,
	val count: Int,
	val policy: String
)
