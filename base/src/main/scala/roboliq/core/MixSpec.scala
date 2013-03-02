package roboliq.core

import RqPimper._


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
	val nVolume_? : Option[LiquidVolume],
	val nCount_? : Option[Integer],
	val mixPolicy_? : Option[PipettePolicy] = None
) {
	def toMixSpec: RqResult[MixSpec] = {
		for {
			volume <- nVolume_?.asRq("MixSpec requires volume")
			count <- nCount_?.asRq("MixSpec requires volume")
			mixPolicy <- mixPolicy_?.asRq("MixSpec requires volume")
		} yield MixSpec(volume, count, mixPolicy)
	}
		
	/**
	 * Merge two mix specs by using the values of `that` for any empty values of `this`.
	 */
	def +(that: MixSpecOpt): MixSpecOpt = {
		MixSpecOpt(
			if (nVolume_?.isEmpty) that.nVolume_? else nVolume_?,
			if (nCount_?.isEmpty) that.nCount_? else nCount_?,
			if (mixPolicy_?.isEmpty) that.mixPolicy_? else mixPolicy_?
		)
	}
}
