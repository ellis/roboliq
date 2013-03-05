package roboliq.commands.pipette

import scala.reflect.runtime.{universe => ru}
import roboliq.core._
import roboliq.events._
import RqPimper._
import roboliq.processor._
import scala.collection.JavaConversions._
import scala.Option.option2Iterable
import spray.json._


case class DispenseCmd(
	description_? : Option[String],
	items: List[TipWellVolumePolicy] //FIXME: This should be TipWellVolumePolicyMixspec
)

case class DispenseToken(
	val items: List[TipWellVolumePolicy]
) extends CmdToken

class DispenseHandler extends CommandHandler("pipetter.dispense") {
	val fnargs = cmdAs[DispenseCmd] { cmd =>
		val events = cmd.items.flatMap(item => {
			TipDispenseEvent(item.tip, item.well.vesselState, item.volume, item.policy.pos) ::
			VesselRemoveEvent(item.well, item.volume) :: Nil
		})
		//val (doc, docMarkdown) = SpirateTokenItem.toAspriateDocString(cmd.items, ctx.ob, ctx.states)
		//Expand2Tokens(List(new AspirateToken(lItem.toList)), events.toList, doc, docMarkdown)
		RqSuccess(List(
			ComputationItem_Token(DispenseToken(cmd.items)),
			ComputationItem_Events(events)
		))
	}
}
