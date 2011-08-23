package roboliq.common

class SetupConfigState(val setup: ObjSetup, val config: ObjConfig, val state0: ObjState)

class ObjMapper(
	val map: Map[Obj, SetupConfigState]
) {
	def configL2(o: Obj) = map.get(o) match {
		case Some(v) => Some(v.config)
		case None => None
	}
	def state0L2(o: Obj) = map.get(o) match {
		case Some(v) => Some(v.state0)
		case None => None
	}
	def configL4(o: Obj) = map.get(o) match {
		case Some(v) => Some(v.setup)
		case None => None
	}
	def state0L4(o: Obj) = map.get(o) match {
		case Some(v) => Some(v.setup)
		case None => None
	}
	
	def createRobotState(): RobotState = {
		val mapStates = map.map(pair => pair._1 -> pair._2.state0).toMap
		new RobotState(mapStates)
	}
}