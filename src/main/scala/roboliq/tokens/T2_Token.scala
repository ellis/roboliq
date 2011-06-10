package roboliq.tokens

import roboliq.parts._

sealed abstract class T2_Token
case class T2_PipetteLiquid(srcs: Array[Well], dests: Array[Well], volumes: Array[Double]) extends T2_Token
