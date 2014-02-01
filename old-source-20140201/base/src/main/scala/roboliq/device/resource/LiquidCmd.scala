package roboliq.device.resource

import scala.reflect.runtime.{universe => ru}
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._


case class LiquidCmd(
	description_? : Option[String],
	id: String,
	content: VesselContent,
	vessels: List[VesselState]
) extends Cmd {
	def cmd = "resource.liquid"
	def typ = ru.typeOf[this.type]
}

class LiquidHandler extends CommandHandler[LiquidCmd]("resource.liquid") {
	def handleCmd(cmd: LiquidCmd): RqReturn = {
		output(
			entityToReturnBuilder(Source(id, cmd.vessels.map(_.vessel))),
			entitiesToReturnBuilder(cmd.vessels.map(state => {
				state.copy(content = cmd.content, isSource_? = Some(true))
			}))
		)
	}
}
