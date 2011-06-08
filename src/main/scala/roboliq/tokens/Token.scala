package roboliq.tokens

import roboliq.parts._


object TipPart extends Enumeration {
	val Inside, Outside = Value
}

object ContaminationDegree extends Enumeration {
	val None, Minor, Medium, Major = Value
}

sealed class TipWellVolume(val tip: Tip, val well: Well, val nVolume: Double)
sealed class TipContamination(val tip: Tip, val part: TipPart.Value, val contam: ContaminationDegree.Value, val nExtent: Double)

sealed class Token
case class Aspirate(twvs: Seq[TipWellVolume], rule: AspirateStrategy) extends Token
case class Dispense(twvs: Seq[TipWellVolume], rule: DispenseStrategy) extends Token
case class Clean(tcs: Seq[TipContamination])
