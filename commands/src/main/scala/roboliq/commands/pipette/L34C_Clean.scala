package roboliq.commands.pipette

import roboliq.common._


case class L3C_Clean(tips: Set[TipConfigL2], degree: CleanDegree.Value) extends Command
