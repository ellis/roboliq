package roboliq.core


/** Represents a part which can have events and therefore needs state (plate, well, devices) */
trait Part {
	def createState(ob: ObjBase): Object
}

/** Represents a part which can have events and therefore needs state (plate, well, devices) */
trait PartBean extends Bean {
	def createState: Object
}

trait PartStateBean extends Bean {
	@Bea
}