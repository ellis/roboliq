package roboliq.commands.pipette

import roboliq.common._


case class L3C_TipsReplace(items: Seq[L3A_TipsReplaceItem]) extends CommandL3 {
	override def toDebugString = {
		val sTips = TipSet.toDebugString(items.map(_.tip))
		val sTypes = getSeqDebugString(items.map(_.sType_?.getOrElse("None")))
		getClass().getSimpleName() + List(sTips, sTypes).mkString("(", ", ", ")") 
	}
}

class L3A_TipsReplaceItem(
	val tip: TipConfigL2,
	val sType_? : Option[String]
)
