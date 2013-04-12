package roboliq.common

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap


class Location extends Obj { thisObj =>
	type Config = LocationConfig
	type State = LocationState
	
	var location_? : Option[String] = None
	
	override def getLabel(kb: KnowledgeBase): String = {
		location_?.getOrElse("NoLoc")
	}

	def createConfigAndState0(): Result[Tuple2[Config, State]] = {
		val errors = new ArrayBuffer[String]

		if (location_?.isEmpty)
			errors += "location not set"
				
		if (!errors.isEmpty)
			return Error(errors)

		val conf = new LocationConfig(
				obj = this,
				location0 = location_?.get)
		val state = new LocationState(
				conf = conf,
				location = conf.location0
				)
		
		Success(conf, state)
	}

	class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]

		def location = state.location
		def location_=(s: String) { map(thisObj) = state.copy(location = s) }
	}
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
}

class LocationConfig(
	val obj: Location,
	val location0: String
) extends ObjConfig {
	def state(states: StateMap) = obj.state(states)
	override def toString = location0
}

case class LocationState(
	val conf: LocationConfig,
	val location: String
) extends ObjState

class LocationProxy(kb: KnowledgeBase, obj: Location) {
	def location: String = null
	def location_=(s: String) = obj.location_? = Some(s)
}
