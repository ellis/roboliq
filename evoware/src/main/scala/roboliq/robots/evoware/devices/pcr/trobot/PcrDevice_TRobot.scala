package roboliq.robots.evoware.devices.pcr.trobot

import scala.collection.mutable.HashMap

import roboliq.common._


class PcrDevice_TRobot extends Device { thisObj =>
	type Setup = PcrDevice_TRobot.Setup
	type Config = PcrDevice_TRobot.Config
	type State = PcrDevice_TRobot.State
	
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
}

object PcrDevice_TRobot {
	class Setup extends ObjSetup {
		var sLabel_? : Option[String] = Some("TRobot")
		var bUsed = false
		def getLabel(kb: KnowledgeBase): String = sLabel_?.getOrElse("TRobot")
	}
	
	class Config(
		val obj: PcrDevice_TRobot,
		val sLabel: String,
		val bUsed: Boolean
	) extends ObjConfig
	
	case class State(
		val obj: PcrDevice_TRobot,
		val bInitialized: Boolean,
		val bOpen: Boolean,
		val bRunning: Boolean
		//val plate_? : Option[PlateConfigL2]
	) extends ObjState
}
