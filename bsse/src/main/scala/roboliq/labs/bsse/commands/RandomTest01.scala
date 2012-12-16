package roboliq.labs.bsse.commands

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import roboliq.core._
import roboliq.commands.pipette._


class RandomTest01CmdBean extends CmdBean {
	@BeanProperty var tips: java.util.List[String] = null
	@BeanProperty var dye: String = null
	@BeanProperty var water: String = null
	@BeanProperty var plate: String = null
}

class RandomTest01CmdHandler extends CmdHandlerA[RandomTest01CmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonNull("dye")
		messages.paramMustBeNonNull("water")
		messages.paramMustBeNonNull("plate")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		Expand1Resources(
			List(
				NeedSrc(cmd.dye),
				NeedSrc(cmd.water),
				NeedDest(cmd.plate)
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
			plate <- states.findPlate(bean.plate)
			dyeSrc_l <- ctx.ob.findWell2List(bean.dye)
			//dyeSrc_ls <- ctx.ob.findAllIdsContainingSubstance(dye)
			//dyeSrc_l <- Result.mapOver(dyeSrc_ls)(wellId => ctx.ob.findWell2(wellId))
			waterSrc_l <- ctx.ob.findWell2List(bean.water)
			//water <- states.findSubstance(bean.water)
			//waterSrc_ls <- ctx.ob.findAllIdsContainingSubstance(water)
			//waterSrc_l <- Result.mapOver(waterSrc_ls)(wellId => ctx.ob.findWell2(wellId))
		} yield {
			val r = new scala.util.Random(42)
			val vol_l = (50 to 200 by 10).toList
			val d1_l = r.shuffle(List.fill(6)(vol_l).flatten)
			val well_l = (0 to 95).toList
			val well1_l = r.shuffle(well_l)
			
			def doit(vw_l: List[(Int, Int)]): List[CmdBean] = {
				if (vw_l.isEmpty) return Nil
				
				val l = r.shuffle(tip_l).zip(vw_l).zipWithIndex
				val (asp_l, dis_l) = l.map(makeItem).unzip
				
				val asp = new AspirateCmdBean
				asp.items = asp_l
				
				val disp_l: List[CmdBean] = dis_l.map(item => {
					val bean = new DispenseCmdBean
					bean.items = item :: Nil
					bean
				})
				
				(asp :: disp_l) ++ doit(vw_l.drop(tip_l.size))
			}
			
			// Return an item for aspiration and one for dispense
			def makeItem(tuple: ((Tip, (Int, Int)), Int)): (SpirateCmdItemBean, SpirateCmdItemBean) = {
				println("tuple: "+tuple)
				val ((tip, (vol_n, well_i)), step_i) = tuple
				val asp = new SpirateCmdItemBean
				val volume = LiquidVolume.ul(vol_n).l.bigDecimal
				val dye_i = step_i % dyeSrc_l.size
				asp.tip = tip.id
				asp.volume = volume
				asp.well = dyeSrc_l(dye_i).id
				asp.policy = "Water_C_1000"
				val dis = new SpirateCmdItemBean
				dis.tip = tip.id
				dis.volume = volume
				dis.well = Plate.wellId(plate, well_i)
				dis.policy = "Water_C_1000"
				//println("makeItem: "+(asp, dis))
				(asp, dis)
			}
			
			doit(d1_l zip well1_l)
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
