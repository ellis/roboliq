package roboliq.robots.evoware.devices.robopeel

import scala.collection.mutable.HashMap

import roboliq.common._


class RoboPeelDevice(val idDevice: String, val idProgramDefault: String, val location: String) extends PlateDevice { thisObj =>
	type Config = RoboPeelDevice.Config
	type State = RoboPeelDevice.State
	
	var bUsed = false
	def getLabel(kb: KnowledgeBase): String = idDevice
	
	def createConfigAndState0(): Result[Tuple2[Config, State]] = {
		val conf = new Config(this, bUsed)
		val state = new State(this)
		Success(conf, state)
	}
	
	def addKnowledge(kb: KnowledgeBase) {
		// Nothing to do
	}
	
	class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]
		
		/*def open(b: Boolean) {
			val st = state
			map(thisObj) = st.copy(bOpen = b)
		}*/
	}
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
	
	def fixedLocation_? : Option[String] = Some(location)
	def isPlateCompatible(plate: Plate): Boolean = true
	//def isPlatePreMoveRequired(plateState: PlateStateL2): Boolean = true
	//def canAccessPlate(plate: PlateStateL2) =
}

object RoboPeelDevice {
	class Config(
		val obj: RoboPeelDevice,
		val bUsed: Boolean
	) extends ObjConfig
	
	case class State(
		val obj: RoboPeelDevice
	) extends ObjState
}
