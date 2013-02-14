package roboliq.commands2.arm

import scala.reflect.runtime.{universe => ru}
import roboliq.core._
import RqPimper._
import roboliq.processor2._


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

class MovePlateHandler extends CommandHandler("arm.movePlate") {
	val fnargs = cmdAs[MovePlateCmd] { cmd =>
		import cmd._
		for {
			locationSrc <- plate.location_?.asRq(s"plate `${plate.conf.id}` must have an location set.")
		} yield {
			val token = new MovePlateToken(
				deviceId_?,
				plate.conf,
				locationSrc,
				dest)
			List(ComputationItem_Token(token))
		}
	}
}
