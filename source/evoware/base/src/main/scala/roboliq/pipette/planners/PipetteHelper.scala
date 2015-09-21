package roboliq.pipette.planners

import roboliq.entities._

object PipetteHelper {
	
	def choosePreAspirateWashSpec(tipOverrides: TipHandlingOverrides, liquidSrc: Mixture, tipState: TipState, cleanBetweenSameSource_? : Option[CleanIntensity.Value]): WashSpec = {
		chooseWashSpec(
			tipOverrides,
			liquidSrc,
			tipState.destsEntered ++ tipState.srcsEntered,
			tipState,
			cleanBetweenSameSource_?,
			"Aspirate"
		)
	}
	
	def choosePreDispenseWashSpec(tipOverrides: TipHandlingOverrides, liquidSrc: Mixture, liquidDest: Mixture, tipState: TipState): WashSpec = {
		chooseWashSpec(
			tipOverrides,
			liquidDest,
			tipState.destsEntered ++ tipState.srcsEntered,
			tipState,
			None,
			"Dispense"
		)
	}
	
	private def chooseWashSpec(
		tipOverrides: TipHandlingOverrides,
		liquid0: Mixture,
		liquids: Iterable[Mixture],
		tipState: TipState,
		cleanBetweenSameSource_? : Option[CleanIntensity.Value],
		label: String
	): WashSpec = {
		val intensity = {
			val bSameLiquid = !liquids.isEmpty && liquids.forall(_ eq liquid0)
			// If same liquids and a cleaning intensity is specified for same source operations:
			if (bSameLiquid && cleanBetweenSameSource_?.isDefined) {
				//println(s"choosePre${label}WashSpec: SAME: ", liquid0.toShortString, liquids.map(_.toShortString), cleanBetweenSameSource_?)
				cleanBetweenSameSource_?.get
			}
			else {
				tipOverrides.washIntensity_?.getOrElse {
					val policy = liquid0.tipCleanPolicy
					val intensity = CleanIntensity.max(policy.enter, tipState.cleanDegreePending)
					//println(s"choosePre${label}WashSpec: DIFF: ", liquid0.toShortString, policy, tipState.cleanDegreePrev, tipState.cleanDegreePending)
					// If no cleaning has been performed yet at all
					if (tipState.cleanDegreePrev == CleanIntensity.None) policy.enter
					// If tip is not as clean as it needs to be
					else if (tipState.cleanDegree < intensity) intensity
					// Otherwise, tip is already sufficiently clean, so no cleaning required
					else CleanIntensity.None
				}
			}
		}
		val contamInside = tipOverrides.contamInside_? match { case Some(v) => v; case None => tipState.contamInside }
		val contamOutside = tipOverrides.contamOutside_? match { case Some(v) => v; case None => tipState.contamOutside }
		new WashSpec(intensity, contamInside, contamOutside)
	}

}