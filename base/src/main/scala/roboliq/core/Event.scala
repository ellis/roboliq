package roboliq.core

import scala.reflect.BeanProperty

/**
 * Base class for all events which change the state of an object.
 * 
 * Derive your events from [[roboliq.core.EventBeanA]].
 */
abstract class EventBean {
	/** ID of the object of this event */
	@BeanProperty var obj: String = null
	
	/**
	 * Alters the state of `obj` via the `builder`.
	 * 
	 * @return Success() on success, otherwise an Error.
	 */
	def update(builder: StateBuilder): Result[Unit]
}

/**
 * Derivation of EventBean which uses type `A` of the state type of the object of interest
 * to improve static type checking.
 * All events should probably be derived from this class.
 */
abstract class EventBeanA[A <: Object : Manifest] extends EventBean {
	def update(builder: StateBuilder): Result[Unit] = {
		for {
			_ <- Result.getNonNull(obj, "event must have an object ID reference")
			state0 <- findState(obj, builder)
			state1 <- update(state0, builder)
		} yield {
			builder.map(obj) = state1
			//println("state1: "+state1)
		}
	}
	
	/**
	 * Find the state of the object with ID `id`.
	 * 
	 * @param id ID of object.
	 * @param query database which maps object IDs to their state objects.
	 * @return object state if found.
	 */
	protected def findState(id: String, query: StateQuery): Result[A]
	
	/**
	 * Update objects state.
	 * 
	 * @param state0 initial object state.
	 * @param query database which maps object IDs to their state objects, in case the state of other objects needs to be inspected.
	 * @return updated object state if there were no errors.
	 */
	protected def update(state0: A, query: StateQuery): Result[A]
}
