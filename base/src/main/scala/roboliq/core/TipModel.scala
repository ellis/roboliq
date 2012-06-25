package roboliq.core

import scala.collection.mutable.HashMap
import scala.reflect.BeanProperty

/** YAML JavaBean representation of [[roboliq.core.TipModel]]. */
class TipModelBean extends Bean {
	/** ID in database. */
	@BeanProperty var id: String = null
	/** Maximum holding volume. */
	@BeanProperty var volumeMax: java.math.BigDecimal = null
	/** Minimum aspiration volume. */
	@BeanProperty var volumeMin: java.math.BigDecimal = null
}

/**
 * Represents a tip model.
 * 
 * @see [[roboliq.core.Tip]]
 * 
 * @param id ID in database.
 * @param nVolume maximum volume which this tip can hold.
 * @param nVolumeAspirateMin minimum aspiration volume.
 * @param nVolumeWashExtra not currently used.
 * @param nVolumeDeconExtra not currently used.
 */
case class TipModel(
	val id: String,
	val nVolume: LiquidVolume, 
	val nVolumeAspirateMin: LiquidVolume, 
	val nVolumeWashExtra: LiquidVolume,
	val nVolumeDeconExtra: LiquidVolume
)

object TipModel {
	/** Convert [[roboliq.core.TipBean]] to [[roboliq.core.Tip]]. */
	def fromBean(bean: TipModelBean): Result[TipModel] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			volumeMax <- Result.mustBeSet(bean.volumeMax, "volumeMax")
			volumeMin <- Result.mustBeSet(bean.volumeMin, "volumeMin")
		} yield {
			new TipModel(id, LiquidVolume.l(volumeMax), LiquidVolume.l(volumeMin), LiquidVolume.empty, LiquidVolume.empty)
		}
	}
}