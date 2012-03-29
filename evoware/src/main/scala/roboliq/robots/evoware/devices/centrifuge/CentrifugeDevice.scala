package roboliq.robots.evoware.devices.centrifuge

import scala.collection.mutable.HashMap

import roboliq.common._


class CentrifugeDevice(val idDevice: String, val location: String, val nSlots: Int) extends PlateDevice { thisObj =>
	type Config = CentrifugeDevice.Config
	type State = CentrifugeDevice.State
	
	var bUsed = false
	var plate_balance: PlateObj = null
	def getLabel(kb: KnowledgeBase): String = idDevice

	def createConfigAndState0(): Result[Tuple2[Config, State]] = {
		val conf = new Config(
			this,
			bUsed
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
	def isPlateCompatible(plate: Plate): Boolean = true
	//def isPlatePreMoveRequired(plateState: PlateStateL2): Boolean = true
	//def canAccessPlate(plate: PlateStateL2) =
}

object CentrifugeDevice {
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
		val mapPosToPlate = new HashMap[Int, Plate]
	}
}
