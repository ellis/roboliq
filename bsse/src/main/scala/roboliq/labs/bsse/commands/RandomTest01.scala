package roboliq.labs.bsse.commands

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import roboliq.core._
import roboliq.commands.pipette._


class RandomTest01CmdBean extends CmdBean {
	@BeanProperty var dye: String = null
	@BeanProperty var water: String = null
	@BeanProperty var plate: String = null
	//@BeanProperty var locationLid: String = null
	//@BeanProperty var lidHandling: LidHandling.Value
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
				NeedPlate(cmd.plate)
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
		for {
			plate <- states.findPlate(bean.plate)
			dye <- states.findSubstance(bean.dye)
			water <- states.findSubstance(bean.water)
			dyeSrc_ls <- ctx.ob.findAllIdsContainingSubstance(dye)
			waterSrc_ls <- ctx.ob.findAllIdsContainingSubstance(water)
		} yield {
			val r = new scala.util.Random(42)
			val vol_l = 50 to 200 by 10
			val d1_l = r.shuffle(List.fill(6)(vol_l).flatten)
			val tip_l = (0 to 3).toList
			val well_l = (0 to 95).toList
			val well1_l = r.shuffle(well_l)
			
			def doit(vw_l: List[(Int, Int)]) {
				val l = r.shuffle(tip_l).zip(vw_l)
				val (asp_l, dis_l) = l.map(makeItems).unzip
			}
			// Return an item for aspiration and one for dispense
			def makeItem(tuple: (Int, (Int, Int))): (SpirateCmdItemBean, SpirateCmdItemBean) = {
				val (tip_i, (vol_n, well_i)) = tuple
				val asp = new SpirateCmdItemBean
				asp.tip = s"Tip${tip_i + 1}"
				asp.volume = LiquidVolume.ul(vol_n).l.bigDecimal
				asp.well = PlateModel.wellId(plate.model, well_i)
				asp.policy = "Water_C_1000"
				val dis = new SpirateCmdItemBean
				dis.tip = s"Tip${tip_i + 1}"
				dis.volume = LiquidVolume.ul(vol_n).l.bigDecimal
				dis.well = PlateModel.wellId(plate.model, well_i)
				dis.policy = "Water_C_1000"
			}
		}
		Expand2Cmds(...)
		RandomTest01Token.fromBean(cmd, ctx.states) match {
			case Error(ls) => ls.foreach(messages.addError); Expand2Errors()
			case Success(token) =>
				val event = PlateLocationEventBean(token.plate, token.plateDest.id)
				val doc = s"Move plate `${token.plate.id}` to location `${token.plateDest.id}`"
				
				Expand2Tokens(List(token), List(event), doc)
		}
	}
}
