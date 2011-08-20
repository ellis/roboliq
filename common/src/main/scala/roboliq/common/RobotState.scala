package roboliq.common

import scala.collection.mutable.HashMap


trait Device // FIXME: This doesn't belong here -- ellis, 2011-08-18

class RobotState(val map: Map[ObjConfig, ObjState])

class StateBuilder(states: RobotState) {
	val map = HashMap[ObjConfig, ObjState](states.map.toSeq : _*)
	
	/*def toState: RobotState = {
		
	}*/
	val toImmutable = new RobotState(Map(map.toSeq : _*))
}
