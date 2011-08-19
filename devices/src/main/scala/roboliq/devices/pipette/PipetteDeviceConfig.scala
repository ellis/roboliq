package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.commands.pipette._


class PipetteDeviceConfig(
	val tips: SortedSet[Tip],
	val tipGroups: Array[Array[Int]]
)
