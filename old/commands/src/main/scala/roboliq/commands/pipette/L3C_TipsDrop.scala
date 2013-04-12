package roboliq.commands.pipette

import roboliq.common._


case class L3C_TipsDrop(tips: Set[TipConfigL2]) extends CommandL3 {
	override def toDebugString = {
		val sTips = TipSet.toDebugString(tips)
		getClass().getSimpleName() + List(sTips).mkString("(", ", ", ")") 
	}
}
