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
		val l1: List[RqResult[List[Event[Entity]]]] = cmd.items.map(item => {
			//println("DispenseHandler item.tip.content: "+item.tip.content)
			for {
				content <- item.tip.content.scaleToVolume(item.volume)
			} yield {
				val i1: Event[Entity] = TipDispenseEvent(item.tip, item.well, item.volume, item.policy.pos)
				val i2: Event[Entity] = VesselAddEvent(item.well, content)
				val l2: List[Event[Entity]] = List(i1, i2)
				l2
			}
		})
		val l2: RqResult[List[List[Event[Entity]]]] = RqResult.toResultOfList(l1)
		val l3: RqResult[List[Event[Entity]]] = l2.map(_.flatten)
		for {
			//val (doc, docMarkdown) = SpirateTokenItem.toAspriateDocString(cmd.items, ctx.ob, ctx.states)
			events <- l3
			ret <- output(
				DispenseToken(cmd.items),
				events
			)
		} yield ret
	}
}
