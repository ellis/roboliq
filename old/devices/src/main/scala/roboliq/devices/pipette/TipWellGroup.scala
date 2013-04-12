package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._



class TipWellGroup private (
	val seq: Seq[TipWell],
	val tips: SortedSet[TipConfigL2],
	val wells: WellGroup,
	val bUniqueTips: Boolean,
	val bUniqueWells: Boolean
) {
	def add(tw: TipWell): TipWellGroup = {
		if (seq.isEmpty) {
			new TipWellGroup(
				Seq(tw), SortedSet(tw.tip), WellGroup(tw.well), true, true
			)
		}
		else {
			new TipWellGroup(
				seq :+ tw, tips + tw.tip, wells + tw.well, !tips.contains(tw.tip), !wells.set.contains(tw.well)
			)
		}
	}
	
	def +(tw: TipWell): TipWellGroup = add(tw)
}

object TipWellGroup {
	val empty = new TipWellGroup(Seq(), SortedSet(), WellGroup.empty, false, false)
	
	def apply(tw: TipWell): TipWellGroup = empty + tw
	def apply(tws: Iterable[TipWell]): TipWellGroup = tws.foldLeft(empty) { (acc, tw) => acc + tw }
}
