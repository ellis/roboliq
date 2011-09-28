package roboliq.robots.evoware.devices.centrifuge

import scala.collection.mutable.HashMap

import roboliq.common._


class CentrifugeDevice(val idDevice: String, val idProgramDefault: String, val location: String, val nSlots: Int) extends PlateDevice { thisObj =>
	type Setup = CentrifugeDevice.Setup
	type Config = CentrifugeDevice.Config
	type State = CentrifugeDevice.State
	
	val setup = new Setup(this)
	
	def createSetup(): Setup = setup
	
	def createConfigAndState0(setup: Setup): Result[Tuple2[Config, State]] = {
		val conf = new Config(
			this,
			setup.bUsed
		)
		val state = new State(
			this,
			false,
			None,
			None
		)
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

object CentrifugeDevice {
	class Setup(obj: CentrifugeDevice) extends ObjSetup {
		var bUsed = false
		var plate_balance: Plate = null
		def getLabel(kb: KnowledgeBase): String = obj.idDevice
	}
	
	class Config(
		val obj: CentrifugeDevice,
		val bUsed: Boolean
	) extends ObjConfig
	
	case class State(
		val obj: CentrifugeDevice,
		val bInitialized: Boolean,
		val bOpen_? : Option[Boolean],
		val iPosition_? : Option[Int]
	) extends ObjState {
		val mapPosToPlate = new HashMap[Int, PlateConfigL2]
	}
}
