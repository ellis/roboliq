package roboliq.core

import scala.language.implicitConversions


case class VesselState(
	vessel: Vessel,
	content: VesselContent = VesselContent.Empty,
	isInitialVolumeKnown_? : Option[Boolean] = None
) {
	assert(isInitialVolumeKnown_? != null)
	def id = vessel.id
	def isInitialVolumeKnown = isInitialVolumeKnown_?.getOrElse(false)

	def liquid = content.liquid
	def volume = content.volume
}

case class VesselSituatedState(
	vesselState: VesselState,
	position: VesselPosition
) extends Ordered[VesselSituatedState] {
	def vessel = vesselState.vessel
	def id = vessel.id
	def plate = position.plate
	def content = vesselState.content
	def liquid = content.liquid
	def volume = content.volume
	def isInitialVolumeKnown = vesselState.isInitialVolumeKnown
	
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

	override def compare(that: VesselSituatedState) = id.compare(that.id)
}

object VesselSituatedState {
	implicit def toVessel(o: VesselSituatedState): Vessel = o.vessel
	implicit def toVesselState(o: VesselSituatedState): VesselState = o.vesselState
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
