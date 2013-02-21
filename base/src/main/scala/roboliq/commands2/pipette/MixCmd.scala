package roboliq.commands2.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._
import roboliq.core.RqPimper._
import roboliq.commands.pipette.{HasTip,HasWell,HasVolume,HasPolicy}
import roboliq.processor2._


case class MixCmd(
	description_? : Option[String],
	items: List[MixItem],
	mixSpec_? : Option[MixSpecOpt]
)

case class MixItem(
	tip: Tip,
	well: Well,
	mixSpec_? : Option[MixSpecOpt]
)

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

class MixHandler extends CommandHandler("pipetter.dispense") {
	val fnargs = cmdAs[MixCmd] { cmd =>
		val event_l = cmd.items.flatMap(item => {
			TipAspirateEvent(item.tip, item.well.vesselState, LiquidVolume.empty) :: Nil
			//WellAddEventBean(item.well, src, item.volume) :: Nil
		})
		//val (doc, docMarkdown) = SpirateTokenItem.toAspriateDocString(cmd.items, ctx.ob, ctx.states)
		//Expand2Tokens(List(new AspirateToken(lItem.toList)), events.toList, doc, docMarkdown)
		for {
			item_l <- RqResult.toResultOfList(cmd.items.map(item => toTokenItem(cmd.mixSpec_?, item)))
		} yield {
			List(
				ComputationItem_Token(MixToken(item_l)),
				ComputationItem_Events(event_l)
			)
		}
	}
	
	private def toTokenItem(mixSpec0_? : Option[MixSpecOpt], item0: MixItem): RqResult[MixTokenItem] = {
		val mixSpecOpt_? : RqResult[MixSpecOpt] = (mixSpec0_?, item0.mixSpec_?) match {
			case (None, None) => RqError("A MixSpec must be specified")
			case (Some(x), None) => RqSuccess(x)
			case (None, Some(y)) => RqSuccess(y)
			case (Some(x), Some(y)) => RqSuccess(y + x)
		}
		for {
			mixSpecOpt <- mixSpecOpt_?
			volume <- mixSpecOpt.nVolume_?.asRq("mix volume must be specified")
			count <- mixSpecOpt.nCount_?.asRq("mix count must be specified")
			policy <- mixSpecOpt.mixPolicy_?.asRq("mix policy must be specified")
		} yield {
			MixTokenItem(item0.tip, item0.well, volume, count, policy) 
		}
	}
}
