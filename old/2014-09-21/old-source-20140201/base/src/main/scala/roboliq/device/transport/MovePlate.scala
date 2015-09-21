package roboliq.device.transport

import scala.reflect.runtime.{universe => ru}
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._


case class MovePlateCmd(
	plate: PlateState,
	destination: PlateLocation,
	deviceId_? : Option[String]
) {
	def cmd = "transport.movePlate"
	def typ = ru.typeOf[this.type]
}

case class MovePlateToken(
	val deviceId_? : Option[String],
	val plate: Plate,
	val plateSrc: PlateLocation,
	val plateDest: PlateLocation
) extends CmdToken

class MovePlateHandler extends CommandHandler[MovePlateCmd]("transport.movePlate") {
	def handleCmd(cmd: MovePlateCmd) = {
		val plate = cmd.plate
		for {
			locationSrc <- plate.location_?.asRq(s"plate `${plate.plate.id}` must have a location set.")
			ret <- output(
				new MovePlateToken(
					cmd.deviceId_?,
					plate.plate,
					locationSrc,
					cmd.destination
				),
				PlateLocationEvent(plate, cmd.destination)
			)
		} yield ret
	}
}
