package roboliq.robots.evoware

import roboliq.core._
import roboliq.commands.pipette._


object Utils {
	/*
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
	*/
	// Assert that tips are spaced at equal distances to each other as the wells are to each other
	def equidistant2(a: Tuple2[HasTipWell, WellInfo], b: Tuple2[HasTipWell, WellInfo]): Boolean =
		(b._1.tip.index - a._1.tip.index) == (b._2.index - a._2.index)
		
	// Test all adjacent items for equidistance
	def equidistant2(tws: Seq[Tuple2[HasTipWell, WellInfo]]): Boolean = tws match {
		case Seq() => true
		case Seq(_) => true
		case Seq(a, b, rest @ _*) =>
			equidistant2(a, b) match {
				case false => false
				case true => equidistant2(Seq(b) ++ rest)
			}
	}
	
	// Test all adjacent items for equidistance
	def equidistant(items: Seq[HasTipWell], query: StateQuery): Boolean = {
		val lWellInfo = items.map(item => WellInfo(item.well, query)).toList
		val l = items zip lWellInfo
		equidistant2(l)
	}
}
