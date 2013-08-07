package roboliq.pipette

import scala.collection.mutable.HashMap
import scala.reflect.BeanProperty

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
	val cleanIntensityToExtraVolume: Map[CleanIntensity.Value, LiquidVolume] = Map()
) extends Entity {
	val volumeWashExtra: LiquidVolume = cleanIntensityToExtraVolume.getOrElse(CleanIntensity.Thorough, LiquidVolume.empty)
	val volumeDeconExtra: LiquidVolume = cleanIntensityToExtraVolume.getOrElse(CleanIntensity.Decontaminate, LiquidVolume.empty)
}
