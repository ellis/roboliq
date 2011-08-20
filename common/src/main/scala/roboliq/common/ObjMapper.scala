package roboliq.common

class SetupConfigState(val setup: ObjSetup, val config: ObjConfig, val state0: ObjState)

class ObjMapper(
	val map: Map[Obj, SetupConfigState]
) {
	def configL1(o: Obj) = map.get(o) match {
		case Some(v) => Some(v.config)
		case None => None
	}
	def state0L1(o: Obj) = map.get(o) match {
		case Some(v) => Some(v.state0)
		case None => None
	}
	def configL3(o: Obj) = map.get(o) match {
		case Some(v) => Some(v.setup)
		case None => None
	}
	def state0L3(o: Obj) = map.get(o) match {
		case Some(v) => Some(v.setup)
		case None => None
	}
	
	def createRobotState(): RobotState = {
		val mapStates = map.mapValues(scs => scs.state0)
		new RobotState(mapStates)
	}
}