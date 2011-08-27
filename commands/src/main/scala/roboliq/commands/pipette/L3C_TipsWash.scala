package roboliq.commands.pipette

import roboliq.common._


class L3C_TipsWash(items: Seq[L3A_TipsWashItem]) extends CommandL3

class L3A_TipsWashItem(
	val tip: TipConfigL2,
	val washSpec: WashSpec
)
