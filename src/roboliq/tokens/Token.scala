package roboliq.tokens

import roboliq.parts._


sealed class TipWellVolume(val tip: Tip, val well: Well, val nVolume: Double)

sealed abstract class Token
case class Aspirate(twvs: Seq[TipWellVolume], rule: AspirateStrategy) extends Token
case class Dispense(twvs: Seq[TipWellVolume], rule: DispenseStrategy) extends Token
