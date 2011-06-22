package roboliq.level2.tokens

import scala.collection.immutable.SortedSet
import roboliq.parts._

abstract class Token
case class T2_PipetteLiquid(srcs: SortedSet[Well], mapDestAndVolume: Map[Well, Double]) extends Token
