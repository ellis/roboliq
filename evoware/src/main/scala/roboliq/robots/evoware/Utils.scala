package roboliq.robots.evoware

import roboliq.core._
import roboliq.commands.pipette._


object Utils {
	// Assert that tips are spaced at equal distances to each other as the wells are to each other
	def equidistant(a: HasTipWell, b: HasTipWell): Boolean =
		(b.tip.index - a.tip.index) == (b.well.index - a.well.index)
	
	// Test all adjacent items for equidistance
	def equidistant(item: Seq[HasTipWell]): Boolean = item match {
		case Seq() => true
		case Seq(_) => true
		case Seq(a, b, rest @ _*) =>
			equidistant(a, b) match {
				case false => false
				case true => equidistant(Seq(b) ++ rest)
			}
	}
}
