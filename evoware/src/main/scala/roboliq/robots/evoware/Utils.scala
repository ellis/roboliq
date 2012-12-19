package roboliq.robots.evoware

import roboliq.core._
import roboliq.commands.pipette._


object Utils {
	// Test all adjacent items for equidistance
	def equidistant(items: Seq[HasTip with HasWell]): Boolean = {
		val lWellInfo = items.map(item => WellInfo(item.well)).toList
		val l = items zip lWellInfo
		equidistant2(l)
	}
		
	// Test all adjacent items for equidistance
	def equidistant2(tws: Seq[Tuple2[HasTip, WellInfo]]): Boolean = tws match {
		case Seq() => true
		case Seq(_) => true
		case Seq(a, b, rest @ _*) =>
			equidistant3(a, b) match {
				case false => false
				case true => equidistant2(Seq(b) ++ rest)
			}
	}
	
	// All tip/well pairs are equidistant or all tips are going to the same well
	// Assert that tips are spaced at equal distances to each other as the wells are to each other
	def equidistant3(a: Tuple2[HasTip, WellInfo], b: Tuple2[HasTip, WellInfo]): Boolean =
		(b._1.tip.index - a._1.tip.index) == (b._2.index - a._2.index)
}
