package roboliq.commands.pipette

import roboliq.common._


case class L3C_TipsWash(items: Seq[L3A_TipsWashItem], intensity: WashIntensity.Value) extends CommandL3

class L3A_TipsWashItem(
	val tip: TipConfigL2,
	val contamInside: Set[Contaminant.Value],
	val contamOutside: Set[Contaminant.Value]
)
