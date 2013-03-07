package roboliq.entity


/**
 * Represents a plate or rack model.
 * 
 * @param id ID in database.
 * @param nRows number of rows on the plate.
 * @param nCols number of columns on the plate.
 * @param nWellVolume maximum volume that can go in the wells.
 */
case class PlateModel(
	val id: String,
	val rows: Int,
	val cols: Int,
	val wellVolume: LiquidVolume
)

object PlateModel {
	/** Get a row/column representation of the index of the a well. */
	@deprecated("use WellSpecParser.wellIndexName() instead", "0.1")
	def wellIndexName(nRows: Int, nCols: Int, iWell: Int): String = {
		if (nCols == 1) {
			if (nRows == 1) {
				""
			}
			else {
				(iWell + 1).toString
			}
		} else {
			val iRow = iWell % nRows
			val iCol = iWell / nRows
			(iRow + 'A').asInstanceOf[Char].toString + ("%02d".format(iCol + 1))
		}
	}
}
