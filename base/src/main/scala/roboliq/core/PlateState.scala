package roboliq.core

import scala.reflect.BeanProperty


/** State of a [[roboliq.core.Plate]]. */
case class PlateState(
	val plate: Plate,
	val location_? : Option[PlateLocation]
) {
	def id = plate.id
}

/** Factory object for [[roboliq.core.PlateState]]. */
object PlateState {
	/** Create an initial state for `plate` with no location. */
	def createEmpty(plate: Plate): PlateState = {
		PlateState(plate, None)
	}
}

/*
/** Convenience class for modifying plate state. */
class PlateStateWriter(o: Plate, builder: StateBuilder) {
	def state = builder.map(o.id).asInstanceOf[PlateState]
	
	private def set(state1: PlateState) { builder.map(o.id) = state1 }
	
	def location = state.location
	def location_=(location: String) { set(state.copy(location = location)) }
}
*/

/** Represents a plate events. */
abstract class PlateEventBean extends EventBeanA[PlateState] {
	protected def findState(id: String, query: StateQuery): Result[PlateState] = {
		query.findPlateState(id)
	}
}

/** Represents an aspiration event. */
class PlateLocationEventBean extends PlateEventBean {
	/** ID of the new location. */
	@BeanProperty var location: String = null
	
	protected def update(state0: PlateState, query: StateQuery): Result[PlateState] = {
		for {
			location <- query.findPlateLocation(location)
		} yield {
			new PlateState(
				state0.plate,
				location_? = Some(location)
			)
		}
	}
}

/** Factory object for [[roboliq.core.PlateLocationEventBean]]. */
object PlateLocationEventBean {
	def apply(plate: Plate, location: String): PlateLocationEventBean = {
		val bean = new PlateLocationEventBean
		bean.obj = plate.id
		bean.location = location
		bean
	}
}