package roboliq.labs.bsse.commands

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import roboliq.core._
import roboliq.commands.pipette._


class RandomFill01CmdBean extends CmdBean {
	@BeanProperty var tips: java.util.List[String] = null
	@BeanProperty var src: String = null
	@BeanProperty var dest: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var policy: String = null
}

class RandomFill01CmdHandler extends CmdHandlerA[RandomFill01CmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonNull("tips")
		messages.paramMustBeNonNull("src")
		messages.paramMustBeNonNull("dest")
		messages.paramMustBeNonNull("volume")
		messages.paramMustBeNonNull("policy")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		Expand1Resources(
			List(
				NeedSrc(cmd.src),
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
			src_l <- states.mapIdsToWell2Lists(bean.src).map(_.flatten)
			volume: BigDecimal = bean.volume
		} yield {
			val r = new scala.util.Random(42)
			val d1_l = List.fill(96)(LiquidVolume.l(volume))
			val dest_l = r.shuffle(dest0_l)
			
			def doit(vw_l: List[(LiquidVolume, Well2)]): List[CmdBean] = {
				if (vw_l.isEmpty) return Nil
				
				val l = r.shuffle(tip_l).zip(vw_l).zipWithIndex
				val (asp_l, dis_l, lvl_l) = l.map(makeItem).unzip3
				
				val asp = new AspirateCmdBean
				asp.items = asp_l
				
				val disp_l: List[CmdBean] = (dis_l zip lvl_l).flatMap(pair => {
					val (ditem, litem) = pair
					val dbean = new DispenseCmdBean
					dbean.items = ditem :: Nil
					val lbean = new DetectLevelCmdBean
					lbean.items = litem :: Nil
					List(dbean, lbean)
				})
				
				(asp :: disp_l) ++ doit(vw_l.drop(tip_l.size))
			}
			
			// Return an item for aspiration and one for dispense
			def makeItem(tuple: ((Tip, (LiquidVolume, Well2)), Int)): (SpirateCmdItemBean, SpirateCmdItemBean, DetectLevelCmdItemBean) = {
				//println("tuple: "+tuple)
				val ((tip, (volume, well)), step_i) = tuple
				val asp = new SpirateCmdItemBean
				val src_i = tip.index % src_l.size
				asp.tip = tip.id
				asp.volume = volume.l.bigDecimal
				asp.well = src_l(src_i).id
				asp.policy = cmd.policy
				val dis = new SpirateCmdItemBean
				dis.tip = tip.id
				dis.volume = volume.l.bigDecimal
				dis.well = well.id
				dis.policy = cmd.policy
				//println("makeItem: "+(asp, dis))
				val lvl = new DetectLevelCmdItemBean
				lvl.tip = tip.id
				lvl.well = well.id
				lvl.policy = cmd.policy
				(asp, dis, lvl)
			}
			
			doit(d1_l zip dest_l)
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
