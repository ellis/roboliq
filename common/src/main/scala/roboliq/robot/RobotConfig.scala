package roboliq.robot

import scala.collection.immutable.SortedSet
import roboliq.parts._


class RobotConfig(
		val tips: SortedSet[Tip],
		val tipGroups: Array[Array[Int]]
)
