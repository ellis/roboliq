package roboliq.core

import scala.reflect.BeanProperty


/** Represents a plate model in YAML */
class PlateModelBean extends Bean {
	/** Number of rows on the plate */
	@BeanProperty var rows: java.lang.Integer = null
	/** Number of columns on the plate */
	@BeanProperty var cols: java.lang.Integer = null
	/** Volume of wells in liters */
	@BeanProperty var volume: java.math.BigDecimal = null
}

class PlateModel(
	val id: String,
	val nRows: Int,
	val nCols: Int,
	val nWellVolume: Double
)

object PlateModel {
	def fromBean(bean: PlateModelBean): Result[PlateModel] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			nRows <- Result.mustBeSet(bean.rows, "rows")
			nCols <- Result.mustBeSet(bean.cols, "cols")
			nWellVolume <- Result.mustBeSet(bean.volume, "volume")
		} yield {
			new PlateModel(id, nRows, nCols, nWellVolume.doubleValue())
		}
	}
	
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
