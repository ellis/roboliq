package roboliq.core

import scala.reflect.BeanProperty


/** YAML JavaBean representation of [[roboliq.core.TubeModel]]. */
class TubeModelBean extends Bean {
	/** Maximum volume which the tube can hold in liters */
	@BeanProperty var volume: java.math.BigDecimal = null
}

/**
 * Represents a tube model.
 * 
 * @see [[roboliq.core.Tube]]
 * 
 * @param id ID in database.
 * @param volume maximum volume which the tube can hold.
 */
case class TubeModel(
	val id: String,
	val volume: LiquidVolume
)

object TubeModel {
	/** Convert [[roboliq.core.TipModelBean]] to [[roboliq.core.TipModel]]. */
	def fromBean(bean: TubeModelBean): Result[TubeModel] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			nVolume <- Result.mustBeSet(bean.volume, "volume")
		} yield {
			new TubeModel(id, LiquidVolume.l(nVolume))
		}
	}
}
