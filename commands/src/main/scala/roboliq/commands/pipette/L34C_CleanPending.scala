package roboliq.commands.pipette

import scala.collection.immutable.SortedSet
import roboliq.common._


case class L3C_CleanPending(tips: SortedSet[TipConfigL2]) extends CommandL3 {
	override def toDebugString = {
		val sTips = TipSet.toDebugString(tips)
		getClass().getSimpleName() + List(sTips).mkString("(", ", ", ")") 
	}
}
