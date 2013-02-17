package roboliq.core

case class Vessel0(
	id: String,
	tubeModel_? : Option[TubeModel]
)

case class VesselState(
	vessel: Vessel0,
	position_? : Option[VesselPosition],
	content: VesselContent
)

/**
 * Represents a [[roboliq.core.Tube]] position on a particular rack.
 * 
 * @param id ID of vessel in database.
 * @param idPlate ID of plate in database.
 * @param index Index of well on plate.
 * @param iRow Index or well's row on plate (0-based).
 * @param iCol Index or well's column on plate (0-based).
 * @param indexName String representation of the well's plate location.
 */
case class VesselPosition(
	plate: PlateState,
	index: Int
)
