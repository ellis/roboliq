package roboliq.labs.bsse.commands

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import roboliq.core._
import roboliq.commands.pipette._


class RandomFill02CmdBean extends CmdBean {
	@BeanProperty var tips: java.util.List[String] = null
	@BeanProperty var src1: String = null
	@BeanProperty var src2: String = null
	@BeanProperty var dest: String = null
	@BeanProperty var volumes1: java.util.List[java.math.BigDecimal] = null
	@BeanProperty var volumes2: java.util.List[java.math.BigDecimal] = null
	@BeanProperty var policy: String = null
}

class RandomFill02CmdHandler extends CmdHandlerA[RandomFill02CmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonNull("tips")
		messages.paramMustBeNonNull("src1")
		messages.paramMustBeNonNull("src2")
		messages.paramMustBeNonNull("dest")
		messages.paramMustBeNonNull("volumes1")
		messages.paramMustBeNonNull("volumes2")
		messages.paramMustBeNonNull("policy")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		Expand1Resources(
			List(
				NeedSrc(cmd.src1),
				NeedSrc(cmd.src2),
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
			src1_l <- states.mapIdsToWell2Lists(bean.src1).map(_.flatten)
			src2_l <- states.mapIdsToWell2Lists(bean.src2).map(_.flatten)
		} yield {
			val r = new scala.util.Random(42)
			val volume1_l = bean.volumes1.toList.map(n => LiquidVolume.l(n))
			val volume2_l = bean.volumes2.toList.map(n => LiquidVolume.l(n))
			val volumes_l = for {v1 <- volume1_l; v2 <- volume2_l} yield (v1, v2)
			val wv_l = r.shuffle(dest0_l) zip r.shuffle(volumes_l)
			
			println("Fill02: ")
			println(wv_l)
			
			def doit(wv_l: List[(Well2, LiquidVolume)], src_l: List[Well2]): List[CmdBean] = {
				if (wv_l.isEmpty) return Nil
				
				val l = r.shuffle(tip_l).zip(wv_l).zipWithIndex
				val (asp_l, dis_l, lvl_l) = l.map(makeItem(src_l)).unzip3
				
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
				
				(asp :: disp_l) ++ doit(wv_l.drop(tip_l.size), src_l)
			}
			
			// Return an item for aspiration and one for dispense
			def makeItem(src_l: List[Well2])(tuple: ((Tip, (Well2, LiquidVolume)), Int)): (SpirateCmdItemBean, SpirateCmdItemBean, DetectLevelCmdItemBean) = {
				val ((tip, (well, volume)), step_i) = tuple
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
			
			doit(wv_l.map(t => (t._1, t._2._1)), src1_l) ++ doit(wv_l.map(t => (t._1, t._2._2)), src2_l)
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
