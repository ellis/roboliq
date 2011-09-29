package roboliq.robots.evoware.devices.trobot

import scala.collection.mutable.HashMap

import roboliq.common._


class TRobotDevice(val idDevice: String, val location: String) extends PlateDevice { thisObj =>
	type Setup = TRobotDevice.Setup
	type Config = TRobotDevice.Config
	type State = TRobotDevice.State
	
	val setup = new Setup
	
	def createSetup(): Setup = setup
	
	def createConfigAndState0(setup: Setup): Result[Tuple2[Config, State]] = {
		for {
			sLabel <- Result.get(setup.sLabel_?, "label not set")
		} yield {
			val conf = new Config(this, sLabel, setup.bUsed)
			val state = new State(this, false, false, false)
			(conf, state)
		}
	}
	
	def addKnowledge(kb: KnowledgeBase) {
		// Nothing to do
	}
	
	class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]
		
		def open(b: Boolean) {
			val st = state
			map(thisObj) = st.copy(bOpen = b)
		}
		
		/*def insertPlate(plate: PlateConfigL2) {
			val st = state
			map(thisObj) = st.copy(plate_? = Some(plate))
		}
		
		def removePlate() {
			val st = state
			map(thisObj) = st.copy(plate_? = None)
		}*/
	}
	//def stateWriter(map: HashMap[ThisObj, StateL2]) = new StateWriter(this, map)
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)

	def fixedLocation_? : Option[String] = Some(location)
	def isPlateCompatible(plate: PlateConfigL2): Boolean = true
}

object TRobotDevice {
	class Setup extends ObjSetup {
		var sLabel_? : Option[String] = Some("TRobot")
		var bUsed = false
		def getLabel(kb: KnowledgeBase): String = sLabel_?.getOrElse("TRobot")
	}
	
	class Config(
		val obj: TRobotDevice,
		val sLabel: String,
		val bUsed: Boolean
	) extends ObjConfig
	
	case class State(
		val obj: TRobotDevice,
		val bInitialized: Boolean,
		val bOpen: Boolean,
		val bRunning: Boolean
		//val plate_? : Option[PlateConfigL2]
	) extends ObjState
}
