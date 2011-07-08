package roboliq.level2.tokens

import scala.collection.immutable.SortedSet
import roboliq.parts._
import roboliq.tokens._


case class T2_PipetteLiquid(srcs: SortedSet[Well], mapDestAndVolume: Map[Well, Double]) extends T2_Token("pipetteLiquid")
case class T2_Pipette(list: Seq[Tuple3[Well, Well, Double]]) extends T2_Token("pipette")
