package roboliq.commands.arm

import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._


case class MovePlateCmd(
	plate: PlateState,
	dest: PlateLocation,
	deviceId_? : Option[String]
)

case class MovePlateToken(
	val deviceId_? : Option[String],
	val plate: Plate,
	val plateSrc: PlateLocation,
	val plateDest: PlateLocation
) extends CmdToken

class MovePlateHandler extends CommandHandler[MovePlateCmd]("arm.movePlate") {
	def handleCmd(cmd: MovePlateCmd) = {
		import cmd._
		for {
			locationSrc <- plate.location_?.asRq(s"plate `${plate.plate.id}` must have a location set.")
			ret <- output(
				new MovePlateToken(
					deviceId_?,
					plate.plate,
					locationSrc,
					dest
				),
				PlateLocationEvent(cmd.plate, cmd.dest)
			)
		} yield ret
	}
}
