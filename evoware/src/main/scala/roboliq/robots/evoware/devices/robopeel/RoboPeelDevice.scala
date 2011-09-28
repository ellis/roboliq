package roboliq.robots.evoware.devices.robopeel

import scala.collection.mutable.HashMap

import roboliq.common._


class RoboPeelDevice(val idDevice: String, val idProgramDefault: String, val location: String) extends PlateDevice { thisObj =>
	type Setup = RoboPeelDevice.Setup
	type Config = RoboPeelDevice.Config
	type State = RoboPeelDevice.State
	
	val setup = new Setup(this)
	
	def createSetup(): Setup = setup
	
	def createConfigAndState0(setup: Setup): Result[Tuple2[Config, State]] = {
		val conf = new Config(this, setup.bUsed)
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
	def isPlateCompatible(plate: PlateConfigL2): Boolean = true
	//def isPlatePreMoveRequired(plateState: PlateStateL2): Boolean = true
	//def canAccessPlate(plate: PlateStateL2) =
}

object RoboPeelDevice {
	class Setup(obj: RoboPeelDevice) extends ObjSetup {
		var bUsed = false
		def getLabel(kb: KnowledgeBase): String = obj.idDevice
	}
	
	class Config(
		val obj: RoboPeelDevice,
		val bUsed: Boolean
	) extends ObjConfig
	
	case class State(
		val obj: RoboPeelDevice
	) extends ObjState
}
