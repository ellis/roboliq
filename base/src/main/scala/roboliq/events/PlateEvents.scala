package roboliq.events

import roboliq.core._, roboliq.entity._, roboliq.processor._


case class PlateLocationEvent(
	plate: PlateState,
	location: PlateLocation
) extends Event[PlateState] {
	def getStateOrId = Right(plate)
}

class PlateLocationEventHandler extends EventHandlerAB[PlateState, PlateLocationEvent]("plate.location") {
	def handleEvent(state0: PlateState, event: PlateLocationEvent): RqResult[PlateState] = {
		RqSuccess(state0.copy(location_? = Some(event.location)))
	}
}