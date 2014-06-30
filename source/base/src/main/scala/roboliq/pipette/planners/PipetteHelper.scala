package roboliq.pipette.planners

import roboliq.entities._

object PipetteHelper {
	
	def choosePreAspirateWashSpec(tipOverrides: TipHandlingOverrides, liquidSrc: Mixture, tipState: TipState, cleanBetweenSameSource_? : Option[CleanIntensity.Value]): WashSpec = {
		chooseWashSpec(
			tipOverrides,
			liquidSrc,
			tipState.destsEntered ++ tipState.srcsEntered,
			tipState,
			cleanBetweenSameSource_?
		)
	}
	
	def choosePreDispenseWashSpec(tipOverrides: TipHandlingOverrides, liquidSrc: Mixture, liquidDest: Mixture, tipState: TipState): WashSpec = {
		chooseWashSpec(
			tipOverrides,
			liquidDest,
			tipState.destsEntered ++ tipState.srcsEntered,
			tipState,
			None
		)
	}
	
	private def chooseWashSpec(
		tipOverrides: TipHandlingOverrides,
		liquid0: Mixture,
		liquids: Iterable[Mixture],
		tipState: TipState,
		cleanBetweenSameSource_? : Option[CleanIntensity.Value]
	): WashSpec = {
		val intensity = {
			val bDifferentLiquid = liquids.exists(_ ne liquid0)
			// If same liquids and a cleaning intensity is specified for same source operations:
			if (!bDifferentLiquid && cleanBetweenSameSource_?.isDefined)
				cleanBetweenSameSource_?.get
			else {
				tipOverrides.washIntensity_?.getOrElse {
					val policy = liquid0.tipCleanPolicy
					if (tipState.cleanDegreePrev == CleanIntensity.None) policy.enter
					else CleanIntensity.None
				}
			}
		}
		val contamInside = tipOverrides.contamInside_? match { case Some(v) => v; case None => tipState.contamInside }
		val contamOutside = tipOverrides.contamOutside_? match { case Some(v) => v; case None => tipState.contamOutside }
		new WashSpec(intensity, contamInside, contamOutside)
	}

}