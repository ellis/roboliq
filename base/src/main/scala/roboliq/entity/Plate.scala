package roboliq.entity

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap


/**
 * Represents a plate with wells (or a rack with tube slots).
 * 
 * @param id plate ID in the database.
 * @param model plate model.
 * @param locationPermanent_? opitonal location ID if this plate cannot be moved.
 */
case class Plate(
	val id: String,
	val model: PlateModel,
	val locationPermanent_? : Option[String]
) extends Ordered[Plate] {
	/** Number of rows. */
	def nRows: Int = model.rows
	/** Number of columns. */
	def nCols: Int = model.cols
	/** Number of wells. */
	def nWells: Int = nRows * nCols
	
	override def compare(that: Plate) = id.compare(that.id)
	override def toString = id
}

object Plate {
	/** Get a row/column representation of the index of the a well. */
	@deprecated("use WellSpecParser.wellId() instead", "0.1")
	def wellId(plate: Plate, iWell: Int): String = {
		s"${plate.id}(${PlateModel.wellIndexName(plate.nRows, plate.nCols, iWell)})"
	}
}
