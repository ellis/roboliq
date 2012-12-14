package roboliq.core

import scala.reflect.BeanProperty


/** YAML JavaBean representation of [[roboliq.core.PlateModel]]. */
class PlateModelBean extends Bean {
	/** Number of rows on the plate */
	@BeanProperty var rows: java.lang.Integer = null
	/** Number of columns on the plate */
	@BeanProperty var cols: java.lang.Integer = null
	/** Volume of wells in liters */
	@BeanProperty var volume: java.math.BigDecimal = null
}

/**
 * Represents a plate or rack model.
 * 
 * @param id ID in database.
 * @param nRows number of rows on the plate.
 * @param nCols number of columns on the plate.
 * @param nWellVolume maximum volume that can go in the wells.
 */
class PlateModel(
	val id: String,
	val nRows: Int,
	val nCols: Int,
	val nWellVolume: LiquidVolume
)

object PlateModel {
	/** Convert from [[roboliq.core.PlateModelBean]] to [[roboliq.core.PlateModel]]. */
	def fromBean(bean: PlateModelBean): Result[PlateModel] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			nRows <- Result.mustBeSet(bean.rows, "rows")
			nCols <- Result.mustBeSet(bean.cols, "cols")
			nWellVolume <- Result.mustBeSet(bean.volume, "volume")
		} yield {
			new PlateModel(id, nRows, nCols, LiquidVolume.l(nWellVolume))
		}
	}

	/** Get a row/column representation of the index of the a well. */
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

	/** Get a row/column representation of the index of the a well. */
	def wellId(plate: PlateModel, iWell: Int): String = {
		s"${plate.id}(${wellIndexName(plate.nRows, plate.nCols, iWell)})"
	}
}
