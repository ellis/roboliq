package roboliq.core

import scala.collection.mutable.HashMap
import scala.reflect.BeanProperty

class TipModelBean extends Bean {
	@BeanProperty var id: String = null
	@BeanProperty var volumeMax: java.math.BigDecimal = null
	@BeanProperty var volumeMin: java.math.BigDecimal = null
	//@BeanProperty var nVolumeWashExtra: LiquidVolume
	//@BeanProperty var nVolumeDeconExtra: LiquidVolume
}

case class TipModel(
	val id: String,
	val nVolume: LiquidVolume, 
	val nVolumeAspirateMin: LiquidVolume, 
	val nVolumeWashExtra: LiquidVolume,
	val nVolumeDeconExtra: LiquidVolume
)

object TipModel {
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