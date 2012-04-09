package roboliq.core

import scala.reflect.BeanProperty


/** Represents a plate model in YAML */
class TubeModelBean extends Bean {
	/** Volume of wells in liters */
	@BeanProperty var volume: java.math.BigDecimal = null
}

class TubeModel(
	val id: String,
	val volume: LiquidVolume
)

object TubeModel {
	def fromBean(bean: TubeModelBean): Result[TubeModel] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			nVolume <- Result.mustBeSet(bean.volume, "volume")
		} yield {
			new TubeModel(id, LiquidVolume.l(nVolume))
		}
	}
}
