package roboliq.processor

import roboliq.core._


class MovePlateToken(
	val deviceId_? : Option[String],
	val plate: Plate,
	val plateSrc: PlateLocation,
	val plateDest: PlateLocation
) extends CmdToken

class MovePlateCmdHandler extends CommandHandler {
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
}
