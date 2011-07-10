package roboliq.level2.tokens

import scala.collection.immutable.SortedSet
import roboliq.parts._
import roboliq.tokens._


case class T2_Pipette(list: Seq[Tuple3[Well, Well, Double]]) extends T2_Token("pipette")
case class T2_PipetteLiquid(srcs: SortedSet[Well], mapDestAndVolume: Map[Well, Double]) extends T2_Token("pipetteLiquid")
case class T2_PipettePlate(src: Plate, dest: Plate, nVolume: Double) extends T2_Token("pipettePlate")
case class T2_PipetteMix(wells: Seq[Well], nVolume: Double) extends T2_Token("pipetteMix")
