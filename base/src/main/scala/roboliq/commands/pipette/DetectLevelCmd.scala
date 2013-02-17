/*package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class DetectLevelCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[DetectLevelCmdItemBean] = null
}

class DetectLevelCmdHandler extends CmdHandlerA[DetectLevelCmdBean] {
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
			Expand2Tokens(List(new DetectLevelToken(lItem)), Nil, null)
		}
	}
}

class DetectLevelCmdItemBean {
	@BeanProperty var tip: String = null
	@BeanProperty var well: String = null
	@BeanProperty var policy: String = null
	
	def toTokenItem(cmd: DetectLevelCmdBean, ob: ObjBase, node: CmdNodeBean): Option[TipWellPolicy] = {
		for {
			idTip <- node.getValueNonNull_?(tip, "tip")
			idWell <- node.getValueNonNull_?(well, "well")
			idPolicy <- node.getValueNonNull_?(policy, "policy")
			tipObj <- ob.findTip_?(tip, new CmdMessageWriter(node))
			well <- ob.findWell2(idWell, node)
		} yield {
			val policy = PipettePolicy.fromName(idPolicy)
			new TipWellPolicy(tipObj, well, policy)
		}
	}
}

case class DetectLevelToken(
	val items: List[TipWellPolicy]
) extends CmdToken
*/