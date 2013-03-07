package roboliq.commands.pipette.low

import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import scala.collection.JavaConversions._
import spray.json._
import roboliq.commands.pipette._


case class DispenseCmd(
	description_? : Option[String],
	items: List[TipWellVolumePolicy] //FIXME: This should be TipWellVolumePolicyMixspec
)

case class DispenseToken(
	val items: List[TipWellVolumePolicy]
) extends CmdToken

class DispenseHandler extends CommandHandler[DispenseCmd]("pipette.low.dispense") {
	def handleCmd(cmd: DispenseCmd): RqReturn = {
		for {
			//val (doc, docMarkdown) = SpirateTokenItem.toAspriateDocString(cmd.items, ctx.ob, ctx.states)
			events <- RqResult.toResultOfList(cmd.items.map(item => {
				println("DispenseHandler item.tip.content: "+item.tip.content)
				for {
					content <- item.tip.content.scaleToVolume(item.volume)
				} yield {
					TipDispenseEvent(item.tip, item.well, item.volume, item.policy.pos) ::
					VesselAddEvent(item.well, content) :: Nil
				}
			})).map(_.flatten)
			ret <- output(
				DispenseToken(cmd.items),
				events
			)
		} yield ret
	}
}
