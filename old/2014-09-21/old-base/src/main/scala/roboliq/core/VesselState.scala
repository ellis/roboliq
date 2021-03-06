package roboliq.core

case class Vessel0(
	id: String,
	tubeModel_? : Option[TubeModel]
)

case class VesselState(
	vessel: Vessel0,
	content: VesselContent
) {
	def id = vessel.id
}

case class VesselSituatedState(
	vesselState: VesselState,
	position: VesselPosition
) extends Ordered[VesselSituatedState] {
	def vessel = vesselState.vessel
	def id = vessel.id
	def plate = position.plate
	
	/** ID of plate in database. */
	val idPlate: String = position.plate.plate.id
	/** Index of well on plate. */
	val index: Int = position.index
	/** Index or well's row on plate (0-based). */
	val iRow: Int = WellSpecParser.wellRow(position.plate.plate, index)
	/** Index or well's column on plate (0-based). */
	val iCol: Int = WellSpecParser.wellCol(position.plate.plate, index)
	/** String representation of the well's plate location. */
	val indexName: String = WellSpecParser.wellIndexName(position.plate.plate.nRows, position.plate.plate.nCols, iRow, iCol)

	/** Get well's state. */
	def wellState(states: StateMap): Result[WellState] = states.findWellState(id)
	/** Get well's state writer. */
	def stateWriter(builder: StateBuilder): WellStateWriter = new WellStateWriter(id, builder)
	
	override def compare(that: VesselSituatedState) = id.compare(that.id)
}

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
