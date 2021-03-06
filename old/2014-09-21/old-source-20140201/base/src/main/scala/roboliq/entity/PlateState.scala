package roboliq.entity

import scala.language.implicitConversions


/** State of a [[roboliq.core.Plate]]. */
case class PlateState(
	val plate: Plate,
	val location_? : Option[PlateLocation]
) extends Entity {
	def id = plate.id
	/** Number of rows. */
	def rows: Int = plate.nRows
	/** Number of columns. */
	def cols: Int = plate.nCols
	/** Number of wells. */
	def wellCount: Int = plate.nWells
	
	override def toString = s"PlateState(${plate.id}, ${location_?.map(_.id)})"
}

/** Factory object for [[roboliq.core.PlateState]]. */
object PlateState {
	/** Create an initial state for `plate` with no location. */
	def createEmpty(plate: Plate): PlateState = {
		PlateState(plate, None)
	}
	
	implicit def toPlate(o: PlateState): Plate = o.plate
}
