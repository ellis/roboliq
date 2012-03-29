package roboliq.common

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
	def apply(bean: PlateModelBean): Result[PlateModel] = {
		for {
			id <- Result.getNonNull(bean._id, "`_id` is missing")
			nRows <- Result.getNonNull(bean.rows, "`rows` must be set")
			nCols <- Result.getNonNull(bean.cols, "`cols` must be set")
			nWellVolume <- Result.getNonNull(bean.volume, "`volume` must be set")
		} yield {
			new PlateModel(id, nRows, nCols, nWellVolume.doubleValue())
		}
	}
}
