package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.commands.pipette._

class TipSpec(val sName: String, val nVolume: Double, val nVolumeAspirateMin: Double, val nVolumeWashExtra: Double, val nVolumeDeconExtra: Double)

class PipetteDeviceConfig(
	val tipSpecs: Seq[TipSpec],
	val tips: SortedSet[Tip],
	val tipGroups: Seq[Seq[Tuple2[Int, TipSpec]]]
)
