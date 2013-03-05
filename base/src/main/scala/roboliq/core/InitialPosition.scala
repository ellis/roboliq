package roboliq.core

/**
 * Initial location for a tube or plate.
 * Tubes should specify `position_?`, and plates should specify `location_?`.
 */
case class InitialLocation(
	id: String,
	position_? : Option[VesselPosition],
	location_? : Option[PlateLocation]
)