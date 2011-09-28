package roboliq.common

abstract class Device extends Obj {
	def addKnowledge(kb: KnowledgeBase)
}

abstract class PlateDevice extends Device {
	def fixedLocation_? : Option[String]
	def isPlateCompatible(plate: PlateConfigL2): Boolean
	//def isPlatePreMoveRequired(plateState: PlateStateL2): Boolean
	//def canAccessPlate(plate: PlateStateL2)
}
