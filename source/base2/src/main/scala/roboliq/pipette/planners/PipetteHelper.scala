package roboliq.pipette.planners

import roboliq.entities._

object PipetteHelper {
	
	def choosePreAspirateWashSpec(tipOverrides: TipHandlingOverrides, liquidSrc: Mixture, tipState: TipState): WashSpec = {
		chooseWashSpec(
			tipOverrides,
			liquidSrc,
			tipState.destsEntered ++ tipState.srcsEntered,
			tipState
		)
	}
	
	def choosePreDispenseWashSpec(tipOverrides: TipHandlingOverrides, liquidSrc: Mixture, liquidDest: Mixture, tipState: TipState): WashSpec = {
		chooseWashSpec(
			tipOverrides,
			liquidDest,
			tipState.destsEntered ++ tipState.srcsEntered,
			tipState
		)
	}
	
	private def chooseWashSpec(tipOverrides: TipHandlingOverrides, liquid0: Mixture, liquids: Iterable[Mixture], tipState: TipState): WashSpec = {
		val intensity = tipOverrides.washIntensity_?.getOrElse {
			val bDifferentLiquid = liquids.exists(_ ne liquid0)
			val policy = liquid0.tipCleanPolicy
			if (tipState.cleanDegreePrev == CleanIntensity.None) policy.enter
			else if (bDifferentLiquid) CleanIntensity.max(policy.enter, tipState.cleanDegreePending)
			else CleanIntensity.None
		}
		val contamInside = tipOverrides.contamInside_? match { case Some(v) => v; case None => tipState.contamInside }
		val contamOutside = tipOverrides.contamOutside_? match { case Some(v) => v; case None => tipState.contamOutside }
		new WashSpec(intensity, contamInside, contamOutside)
	}

}