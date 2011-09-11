package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.commands.pipette._

class PipetteDeviceConfig(
	val tipSpecs: Seq[TipModel],
	val tips: SortedSet[Tip],
	val tipGroups: Seq[Seq[Tuple2[Int, TipModel]]]
)
