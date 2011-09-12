package roboliq.common

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap


class Location extends Obj { thisObj =>
	type Setup = LocationSetup
	type Config = LocationConfig
	type State = LocationState
	
	def createSetup() = new Setup(this)
	def createConfigAndState0(setup: Setup): Either[Seq[String], Tuple2[Config, State]] = {
		val errors = new ArrayBuffer[String]

		if (setup.location_?.isEmpty)
			errors += "location not set"
				
		if (!errors.isEmpty)
			return Left(errors)

		val conf = new LocationConfig(
				obj = this,
				location = setup.location_?.get)
		val state = new LocationState(
				conf = conf)
		
		Right(conf, state)
	}

	/*class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]
	}
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)*/
}

class LocationSetup(val obj: Location) extends ObjSetup {
	var location_? : Option[String] = None
	
	override def getLabel(kb: KnowledgeBase): String = {
		location_?.getOrElse("NoLoc")
	}
}

class LocationConfig(
	val obj: Location,
	val location: String
) extends ObjConfig {
	override def toString = location
}

class LocationState(val conf: LocationConfig) extends ObjState

class LocationProxy(kb: KnowledgeBase, obj: Location) {
	def location: String = null
	def location_=(s: String) = kb.getLocationSetup(obj).location_? = Some(s)
}
