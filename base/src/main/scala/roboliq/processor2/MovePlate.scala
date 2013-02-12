package roboliq.processor2

import scala.reflect.runtime.{universe => ru}
import roboliq.core._
import RqPimper._


case class MovePlateToken(
	val deviceId_? : Option[String],
	val plate: Plate,
	val plateSrc: PlateLocation,
	val plateDest: PlateLocation
) extends CmdToken

class MovePlateHandler extends CommandHandler {
	val cmd_l = List[String]("movePlate")
	
	def getResult = {
		/*require ('id, 'list) {
			(id: String, list: List[String]) =>
			bank on it
		}*/
		handlerRequire (
			lookupPlate('plate),
			lookupPlateState('plate),
			lookupPlateLocation('dest)
		) {
			(plate, plateState, dest) =>
			for {
				locationSrc <- plateState.location_?.asRq(s"plate `${plate.id}` must have an location set.")
			} yield {
				val s: String = new MovePlateToken(
					None, //deviceId_?,
					plate,
					locationSrc,
					dest).toString
				List(ComputationItem_Token(Token_Comment(s)))
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
	/*
	def makeStep(): Step = {
		find (
			findPlate('plate),
			findPlateState('plate),
			findPlateLocation('plateDest),
			asString_?('deviceId)
			//p("plate") map { plateId => (findPlate(plateId), findPlateState(plateId)) },
			//p("dest") map findPlateLocation,
			//p_?("deviceId")
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
				val event = PlateLocationEventBean(token.plate, token.plateDest.id)
				val doc = s"Move plate `${token.plate.id}` to location `${token.plateDest.id}`"
				Step_SubCommands(List(token), event, doc)
			}
		}
	}
	*/
}
