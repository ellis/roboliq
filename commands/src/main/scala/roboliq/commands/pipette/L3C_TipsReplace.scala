package roboliq.commands.pipette

import roboliq.common._


case class L3C_TipsReplace(items: Seq[L3A_TipsReplaceItem]) extends CommandL3 {
	override def toDebugString = {
		getClass().getSimpleName() + items.sortBy(_.tip.index).map(item => (item.tip.index+1) + "->" + item.sType_?.getOrElse("None")).mkString("(", ", ", ")") 
	}
}

class L3A_TipsReplaceItem(
	val tip: TipConfigL2,
	val sType_? : Option[String]
)
