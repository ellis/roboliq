package roboliq.common

import scala.collection.mutable.HashMap


trait StateMap {
	val map: collection.Map[Obj, ObjState]
	def apply(obj: Obj) = map(obj)
}

class RobotState(val map: Map[Obj, ObjState]) extends StateMap {
	def filterByValueType[State <: ObjState](implicit m: Manifest[State]): Map[Obj, State] = {
		map.filter(pair => m.erasure.isInstance(pair._2)).mapValues(_.asInstanceOf[State])
	}
}

class StateBuilder(states: RobotState) extends StateMap {
	val map = HashMap[Obj, ObjState](states.map.toSeq : _*)
	
	def toImmutable: RobotState = new RobotState(map.toMap)
}
