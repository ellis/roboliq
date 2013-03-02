package roboliq.commands.arm

import scala.reflect.runtime.{universe => ru}
import roboliq.core._
import RqPimper._
import roboliq.processor._


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
			locationSrc <- plate.location_?.asRq(s"plate `${plate.plate.id}` must have a location set.")
		} yield {
			val events = List(
				PlateLocationEvent(cmd.plate.plate, cmd.dest)
			)
			val token = new MovePlateToken(
				deviceId_?,
				plate.plate,
				locationSrc,
				dest)
			List(
				ComputationItem_Token(token),
				ComputationItem_Events(events)
			)
		}
	}
}

case class PlateLocationEvent(
	plate: Plate,
	location: PlateLocation
) extends Event

class PlateLocationEventHandler {// extends EventHandler {
	import RqFunctionHandler._
	
	def fnargs(event: PlateLocationEvent) = {
		fnRequire (lookup[PlateState](event.plate.id)) { state0 =>
			val state_# = PlateState(event.plate, Some(event.location))
			ConversionsDirect.toJson(state_#).map(jsstate =>
				List(EventItem_State(TKP("plateState", event.plate.id, Nil), jsstate))
			)
		}
	}
}