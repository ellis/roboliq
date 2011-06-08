package roboliq.tokens

import roboliq.parts._


sealed class TipWellVolume(val tip: Tip, val well: Well, val nVolume: Double)
sealed class TipContamination(val tip: Tip, val part: TipPart.Value, val contam: ContaminationDegree.Value, val nExtent: Double)

object ContaminationSeverity extends Enumeration {
	val None, Minor, Medium, Major = Value
}
sealed class TipCleanSpec(val tip: Tip,
		val nInsideVolume: Double, val insideSeverity: ContaminationSeverity.Value,
		val nOutsideVolume: Double, val outsideSeverity: ContaminationSeverity.Value)

sealed abstract class Token
case class Aspirate(twvs: Seq[TipWellVolume], rule: AspirateStrategy) extends Token
case class Dispense(twvs: Seq[TipWellVolume], rule: DispenseStrategy) extends Token
case class Clean(specs: Traversable[TipCleanSpec]) extends Token
