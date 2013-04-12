package roboliq.device.resource

import scala.reflect.runtime.{universe => ru}
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._


case class PlateCmd(
	description_? : Option[String],
	id: String,
	model: PlateModel,
	location: PlateLocation
) extends Cmd {
	def cmd = "resource.plate"
	def typ = ru.typeOf[this.type]
}

class PlateHandler extends CommandHandler[PlateCmd]("resource.plate") {
	def handleCmd(cmd: PlateCmd): RqReturn = {
		val plate = Plate(cmd.id, cmd.model, None)
		val plateState = PlateState(plate, Some(cmd.location))
		output(
			entityToReturnBuilder(plate),
			entityToReturnBuilder(plateState)
		)
	}
}
