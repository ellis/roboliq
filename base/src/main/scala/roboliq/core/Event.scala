package roboliq.core

import scala.reflect.BeanProperty

abstract class EventBean {
	/** ID of the object of this event */
	@BeanProperty var obj: String = null
	
	def update(builder: StateBuilder): Result[Unit]
}

abstract class EventBeanA[A <: Object : Manifest] extends EventBean {
	def update(builder: StateBuilder): Result[Unit] = {
		for {
			_ <- Result.getNonNull(obj, "event must have an object ID reference")
			state0 <- findState(obj, builder)
			state1 <- update(state0, builder)
		} yield {
			builder.map(obj) = state1
			println("state1: "+state1)
		}
	}
	
	protected def findState(id: String, query: StateQuery): Result[A]
	
	protected def update(state0: A, query: StateQuery): Result[A]
}
