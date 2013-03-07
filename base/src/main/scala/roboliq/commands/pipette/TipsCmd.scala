/*package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scalaz._
import Scalaz._
import roboliq.core._
import roboliq.events._
import roboliq.core.RqPimper._
import roboliq.processor._


case class TipsCmd(
	description_? : Option[String],
	items: List[TipsItem]
)

case class TipsItem(
	tip: TipState,
	tipModel_? : Option[TipModel],
	cleanIntensity: CleanIntensity.Value
)

class CommandHandlerFunction
Writer[Endo[CommandHandlerFunction], Task]


class TipsHandler_Fixed extends CommandHandler[TipsCmd]("pipette.tips") {
	def doit(cmd: TipsCmd) = for {
		(a, b) <- require(lookup[A](cmd.a), lookup[B](cmd.b), lookupAll[WashProgram])
		_ <- event TipCleanEvent()
		_ <- token MixToken()
	} yield ()
	val fnargs = cmdAs[TipsCmd] { cmd =>
		val event_l = cmd.items.flatMap(item => {
			TipCleanEvent(item.tip, item.well.vesselState, LiquidVolume.empty) :: Nil
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
			volume <- mixSpecOpt.volume_?.asRq("mix volume must be specified")
			count <- mixSpecOpt.count_?.asRq("mix count must be specified")
			policy <- mixSpecOpt.mixPolicy_?.asRq("mix policy must be specified")
		} yield {
			MixTokenItem(item0.tip, item0.well, volume, count, policy) 
		}
	}
}
*/