package roboliq.pipette

import roboliq.core._


case class MixSpec(
	val volume: LiquidVolume,
	val count: Integer,
	val mixPolicy: PipettePolicy
)

/**
 * Specification for mixing the contents of a well (i.e. not mixing together, but making the liquid "well-mixed").
 * 
 * @param nVolume_? optional volume of aspiration (default will be about 70%)
 * @param nCount_? optional number of times to mix (default will be about 4)
 * @param mixPolicy_? optional pipette policy
 */
case class MixSpecOpt(
	val volume_? : Option[LiquidVolume],
	val count_? : Option[Integer],
	val mixPolicy_? : Option[PipettePolicy] = None
) {
	def toMixSpec: RsResult[MixSpec] = {
		for {
			volume <- volume_?.asRs("MixSpec requires volume")
			count <- count_?.asRs("MixSpec requires volume")
			mixPolicy <- mixPolicy_?.asRs("MixSpec requires volume")
		} yield MixSpec(volume, count, mixPolicy)
	}
		
	/**
	 * Merge two mix specs by using the values of `that` for any empty values of `this`.
	 */
	def +(that: MixSpecOpt): MixSpecOpt = {
		MixSpecOpt(
			if (volume_?.isEmpty) that.volume_? else volume_?,
			if (count_?.isEmpty) that.count_? else count_?,
			if (mixPolicy_?.isEmpty) that.mixPolicy_? else mixPolicy_?
		)
	}
}
