package roboliq.tokens

import roboliq.parts._


sealed class TipWellVolume(val tip: Tip, val well: Well, val nVolume: Double)

object RelativeValue3 extends Enumeration {
	val None, Small, Medium, Large = Value
}
sealed class TipCleanSpec(val tip: Tip,
		val nInsideVolume: Double, val insideContamination: RelativeValue3.Value,
		val nOutsideVolume: Double, val outsideContamination: RelativeValue3.Value)

sealed abstract class Token
case class Aspirate(twvs: Seq[TipWellVolume], rule: AspirateStrategy) extends Token
case class Dispense(twvs: Seq[TipWellVolume], rule: DispenseStrategy) extends Token
case class Clean(specs: Traversable[TipCleanSpec]) extends Token
