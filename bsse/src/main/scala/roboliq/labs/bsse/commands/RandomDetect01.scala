package roboliq.labs.bsse.commands

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import roboliq.core._
import roboliq.commands.pipette._


class RandomDetect01CmdBean extends CmdBean {
	@BeanProperty var tips: java.util.List[String] = null
	@BeanProperty var dest: String = null
	@BeanProperty var policy: String = null
}

class RandomDetect01CmdHandler extends CmdHandlerA[RandomDetect01CmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonNull("tips")
		messages.paramMustBeNonNull("dest")
		messages.paramMustBeNonNull("policy")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		Expand1Resources(
			List(
				NeedDest(cmd.dest)
			)
		)
	}

	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		val bean = cmd
		val states = ctx.states
		
		val cmd_l_res = for {
			tip_l <- if (bean.tips != null) Result.mapOver(bean.tips.toList)(states.findTip) else ctx.ob.findAllTips
			dest0_l <- states.mapIdsToWell2Lists(bean.dest).map(_.flatten)
		} yield {
			val r = new scala.util.Random(42)
			val tw0_l = dest0_l.flatMap(dest => tip_l.map(_ -> dest))
			val tw_l = r.shuffle(tw0_l)
			
			def doit(tw_l: List[(Tip, Well)]): List[CmdBean] = {
				if (tw_l.isEmpty) return Nil
				
				val lvl_l = tw_l.map(makeItem)
				
				lvl_l.map(litem => {
					val lbean = new DetectLevelCmdBean
					lbean.items = litem :: Nil
					lbean
				})
			}
			
			// Return an item for aspiration and one for dispense
			def makeItem(tuple: (Tip, Well)): DetectLevelCmdItemBean = {
				//println("tuple: "+tuple)
				val (tip, well) = tuple
				val lvl = new DetectLevelCmdItemBean
				lvl.tip = tip.id
				lvl.well = well.id
				lvl.policy = cmd.policy
				lvl
			}
			
			doit(tw_l)
		}
		
		cmd_l_res match {
			case Success(cmd_l) => 
				Expand2Cmds(cmd_l, Nil, null)
			case Error(ls) =>
				println("Errors: "+ls)
				Expand2Errors()
		}
	}
}
