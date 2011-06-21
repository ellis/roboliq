package roboliq.level2.tokens

import roboliq.parts._

abstract class Token
case class T2_PipetteLiquid(srcs: Iterable[Well], mapDestAndVolume: Map[Well, Double]) extends Token
