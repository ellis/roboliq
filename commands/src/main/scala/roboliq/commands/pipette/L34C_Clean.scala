package roboliq.commands.pipette

import roboliq.common._


case class L3C_Clean(items: Seq[L3A_CleanItem]) extends CommandL3

class L3A_CleanItem(
	val tip: TipConfigL2,
	val replacement: TipReplacementAction.Value,
	val contaminantsToWash: Set[Contaminant.Value],
	val washDegree: WashIntensity.Value
)
