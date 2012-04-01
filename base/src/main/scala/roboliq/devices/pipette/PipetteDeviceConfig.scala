package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.core._
import roboliq.commands.pipette._


class TipBlock(val tTip: SortedSet[Tip], val lTipModels: Seq[TipModel]) {
	val lTip = tTip.toSeq
}

class PipetteDeviceConfig(
	val lTipModel: Seq[TipModel],
	val tips: SortedSet[Tip],
	val tipGroups: Seq[Seq[Tuple2[Int, TipModel]]]
)
