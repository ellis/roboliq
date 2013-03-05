package roboliq.commands.pipette.low

import scala.collection.JavaConversions._
import roboliq.core._
import roboliq.events._
import roboliq.core.RqPimper._
import roboliq.processor._
import roboliq.commands.pipette.HasPolicy
import roboliq.commands.pipette.HasTip
import roboliq.commands.pipette.HasVolume
import roboliq.commands.pipette.HasWell
import scala.reflect.runtime.universe


case class MixCmd(
	description_? : Option[String],
	items: List[MixItem],
	mixSpec_? : Option[MixSpecOpt]
)

case class MixItem(
	tip: TipState,
	well: Well,
	mixSpec_? : Option[MixSpecOpt]
)

case class MixToken(
	val items: List[MixTokenItem]
) extends CmdToken

case class MixTokenItem(
	val tip: TipState,
	val well: Well,
	val volume: LiquidVolume,
	val count: Int,
	val policy: PipettePolicy
) extends HasTip with HasWell with HasVolume with HasPolicy

class MixHandler extends CommandHandler("pipette.low.mix") {
	val fnargs = cmdAs[MixCmd] { cmd =>
		val event_l = cmd.items.flatMap(item => {
			TipMixEvent(item.tip, item.well.vesselState, LiquidVolume.empty) :: Nil
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
