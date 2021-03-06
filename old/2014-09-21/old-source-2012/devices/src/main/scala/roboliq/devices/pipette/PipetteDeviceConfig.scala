package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.commands.pipette._

class TipBlock(val tTip: SortedSet[TipConfigL2], val lTipModels: Seq[TipModel]) {
	val lTip = tTip.toSeq
}

/*
class PipetteDeviceConfig(
	val tips: SortedSet[Tip],
	val tipModels: Seq[TipModel],
	val lTipBlock: Seq[TipBlock]
)
*/

class PipetteDeviceConfig(
	val lTipModel: Seq[TipModel],
	val tips: SortedSet[Tip],
	val tipGroups: Seq[Seq[Tuple2[Int, TipModel]]]
)
