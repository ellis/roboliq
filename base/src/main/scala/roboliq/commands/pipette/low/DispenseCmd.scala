package roboliq.commands.pipette.low

import scala.reflect.runtime.{universe => ru}
import roboliq.core._
import roboliq.events._
import roboliq.core.RqPimper._
import roboliq.processor._
import scala.collection.JavaConversions._
import spray.json._
import roboliq.commands.pipette.TipWellVolumePolicy
import roboliq.core.TipState.toTip
import roboliq.core.VesselSituatedState.toVesselState


case class DispenseCmd(
	description_? : Option[String],
	items: List[TipWellVolumePolicy] //FIXME: This should be TipWellVolumePolicyMixspec
)

case class DispenseToken(
	val items: List[TipWellVolumePolicy]
) extends CmdToken

class DispenseHandler extends CommandHandler("pipette.low.dispense") {
	val fnargs = cmdAs[DispenseCmd] { cmd =>
		for {
			events <- RqResult.toResultOfList(cmd.items.map(item => {
				println("DispenseHandler item.tip.content: "+item.tip.content)
				for {
					content <- item.tip.content.scaleToVolume(item.volume)
				} yield {
					TipDispenseEvent(item.tip, item.well, item.volume, item.policy.pos) ::
					VesselAddEvent(item.well, content) :: Nil
				}
			})).map(_.flatten)
		} yield {
			//val (doc, docMarkdown) = SpirateTokenItem.toAspriateDocString(cmd.items, ctx.ob, ctx.states)
			//Expand2Tokens(List(new AspirateToken(lItem.toList)), events.toList, doc, docMarkdown)
			List(
				ComputationItem_Token(DispenseToken(cmd.items)),
				ComputationItem_Events(events)
			)
		}
	}
}
