package roboliq.common

import scala.collection.mutable.HashMap


trait Device // FIXME: This doesn't belong here -- ellis, 2011-08-18

class RobotState(val map: Map[Obj, ObjState]) {
	def apply(obj: Obj) = map(obj)
	
	def filterByValueType[State <: ObjState]: Map[Obj, State] = map.filter(pair => pair._2.isInstanceOf[State]).mapValues(_.asInstanceOf[State])
}

class StateBuilder(states: RobotState) {
	val map = HashMap[Obj, ObjState](states.map.toSeq : _*)
	
	val toImmutable = new RobotState(Map(map.toSeq : _*))
}
