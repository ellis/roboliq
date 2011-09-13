package roboliq.commands

import roboliq.common._


case class L4C_SaveCurrentLocation(plate: Plate, mem: Memento[String]) extends CommandL4 {
	type L3Type = L3C_SaveCurrentLocation

	def addKnowledge(kb: KnowledgeBase) {
		kb.addPlate(plate)
		kb.addObject(mem)
		val plateSetup = kb.getPlateSetup(plate)
		val memSetup = kb.getMementoSetup(mem)
		memSetup.value_? = plateSetup.location_?
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		val plateState = plate.state(states)
		val memState = mem.state(states)
		Success(L3C_SaveCurrentLocation(plateState.conf, memState.conf))
	}

}

case class L3C_SaveCurrentLocation(plate: PlateConfigL2, mem: MementoConfig[String]) extends CommandL3

case class L2C_SaveCurrentLocation(plate: PlateConfigL2, mem: MementoConfig[String]) extends CommandL2 {
	type L1Type = L1C_SaveCurrentLocation
	
	def updateState(builder: StateBuilder) {
		mem.obj.stateWriter(builder).value = plate.state(builder).location
	}
	
	def toL1(states: RobotState): Result[L1Type] = {
		Success(L1C_SaveCurrentLocation())
	}
	
	override def toDebugString = {
		this.getClass().getSimpleName() + List(plate, mem).mkString("(", ", ", ")") 
	}
}

case class L1C_SaveCurrentLocation() extends CommandL1
