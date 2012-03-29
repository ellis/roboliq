package roboliq.common

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap


class Memento[T] extends Obj { thisObj =>
	type Config = MementoConfig[T]
	type State = MementoState[T]
	
	var value_? : Option[T] = None
	
	override def getLabel(kb: KnowledgeBase): String = {
		value_?.getOrElse("NoValue").toString
	}

	def createConfigAndState0(): Result[Tuple2[Config, State]] = {
		val errors = new ArrayBuffer[String]

		if (value_?.isEmpty)
			errors += "value not set"
				
		if (!errors.isEmpty)
			return Error(errors)

		val conf = new MementoConfig(
				obj = this,
				value0 = value_?.get)
		val state = new MementoState(
				conf = conf,
				value = conf.value0
				)
		
		Success(conf, state)
	}

	class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]

		def value = state.value
		def value_=(v: T) {
			val st = state
			map(thisObj) = new MementoState[T](st.conf, v)
		}
	}
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
}

class MementoConfig[T](
	val obj: Memento[T],
	val value0: T
) extends ObjConfig {
	def state(states: StateMap) = obj.state(states)
	override def toString = "Memento("+value0+")"
}

case class MementoState[T](
	val conf: MementoConfig[T],
	val value: T
) extends ObjState

class MementoProxy[T](kb: KnowledgeBase, obj: Memento[T]) {
	def value: String = null
	def value_=(v: T) = obj.value_? = Some(v)
}
