package roboliq.commands2.arm

import scala.reflect.runtime.{universe => ru}
import roboliq.core._
import RqPimper._
import roboliq.processor2._


case class MovePlateToken(
	val deviceId_? : Option[String],
	val plate: Plate,
	val plateSrc: PlateLocation,
	val plateDest: PlateLocation
) extends CmdToken

class MovePlateHandler extends CommandHandler("movePlate") {
	val getResult = {
		handlerRequire (
			lookupPlate('plate),
			lookupPlateState('plate),
			lookupPlateLocation('dest),
			as[Option[String]]('deviceId)
		) {
			(plate, plateState, dest, deviceId_?) =>
			for {
				locationSrc <- plateState.location_?.asRq(s"plate `${plate.id}` must have an location set.")
			} yield {
				val token = new MovePlateToken(
					deviceId_?,
					plate,
					locationSrc,
					dest)
				List(ComputationItem_Token(token))
			}
		}
		/*
		handlerRequire(as[String]('id), as[List[String]]('list)/*, as[Option[String]]('deviceId)*/) {
			(id: String, list: List[String]) =>
			handlerRequire (lookupPlate(id)) { (plate: Plate) =>
				val l: List[RequireItem[PlateModel]] = list.map(lookupPlateModel(_): RequireItem[PlateModel])
				handlerRequireN (l) { (list2: List[PlateModel]) =>
					handlerReturn(Token_Comment(plate.toString + " on " + list + " to " + list2))
				}
			}
		}*/
	}
}
