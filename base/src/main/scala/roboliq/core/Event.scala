package roboliq.core

import scala.reflect.BeanProperty

abstract class EventBean {
	/** ID of the object of this event */
	@BeanProperty var obj: String = null
	
	def update(builder: StateBuilder)
}

abstract class EventBeanA[A <: Object : Manifest] extends EventBean {
	def update(builder: StateBuilder) {
		val state0 = builder.map(obj).asInstanceOf[A]
		val state1 = update(state0, builder)
		builder.map(obj) = state1
	}
	
	protected def update(state0: A, states0: StateMap): A
}
