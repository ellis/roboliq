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
 * @param volumeMin minimum aspiration volume.
 * @param nVolumeWashExtra volume which must be left unused if tip will be washed instead of discarded.
 * @param nVolumeDeconExtra volume which must be left unused if tip will be decontaminated instead of discarded.
 */
case class TipModel(
	val id: String,
	val volume: LiquidVolume, 
	val volumeMin: LiquidVolume, 
	val nVolumeWashExtra: LiquidVolume,
	val nVolumeDeconExtra: LiquidVolume
)

object TipModel {
	/** Convert [[roboliq.core.TipModelBean]] to [[roboliq.core.TipModel]]. */
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