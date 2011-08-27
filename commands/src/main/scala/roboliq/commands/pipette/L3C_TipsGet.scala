package roboliq.commands.pipette

import roboliq.common._


case class L3C_TipsGet(items: Seq[L3A_TipsGetItem]) extends CommandL3

class L3A_TipsGetItem(
	val tip: TipConfigL2,
	val sType: String
)
