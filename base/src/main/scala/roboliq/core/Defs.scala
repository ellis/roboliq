package roboliq.core

import scala.collection.mutable.ArrayBuffer


abstract class LiquidPropertiesFamily
object LiquidPropertiesFamily {
	case object Water extends LiquidPropertiesFamily
	case object Cells extends LiquidPropertiesFamily
	case object DMSO extends LiquidPropertiesFamily
	case object Decon extends LiquidPropertiesFamily
}

object WashIntensity extends Enumeration {
	val None, Light, Thorough, Decontaminate = Value
	
	def max(a: WashIntensity.Value, b: WashIntensity.Value): WashIntensity.Value = {
		if (a >= b) a else b
	}
	
	def max(l: Traversable[WashIntensity.Value]): WashIntensity.Value = {
		l.foldLeft(WashIntensity.None)((acc, intensity) => max(acc, intensity))
	}
}

class LiquidVolume private (_nl: Int) {
	/** Volume in nanoliters [nl] */ 
	def nl: BigDecimal = _nl
	/** Volume in microliters [ul] */ 
	def ul: BigDecimal = (nl / 1000.0)
	override def toString = {
		if (_nl > 1000000)
			(nl / 1000000).toString + " ml"
		else if (_nl > 1000)
			(nl / 1000).toString + " ul"
		else
			nl.toString + " nl"
	}
}
/** Factory for [[roboliq.protocol.LiquidVolume]] */
object LiquidVolume {
	def nl(n: Int): LiquidVolume = new LiquidVolume(n)
	def ul(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000).toInt)
	def ml(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000000).toInt)
	def l(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000000000).toInt)
}

object PipettePosition extends Enumeration {
	val Free, WetContact, DryContact = Value
}

//case class PipetteSpec(sName: String, aspirate: PipettePosition.Value, dispense: PipettePosition.Value, mix: PipettePosition.Value)

case class PipettePolicy(id: String, pos: PipettePosition.Value)

object TipReplacementPolicy extends Enumeration { // FIXME: Replace this with TipReplacementPolicy following Roboease
	val ReplaceAlways, KeepBetween, KeepAlways = Value
}

class TipHandlingOverrides(
	val replacement_? : Option[TipReplacementPolicy.Value],
	//val washProgram_? : Option[Int],
	val washIntensity_? : Option[WashIntensity.Value],
	val contamInside_? : Option[Set[Contaminant.Value]],
	val contamOutside_? : Option[Set[Contaminant.Value]]
)

object TipHandlingOverrides {
	def apply() = new TipHandlingOverrides(None, None, None, None)
}

class WashSpec(
	val washIntensity: WashIntensity.Value,
	val contamInside: Set[Contaminant.Value],
	val contamOutside: Set[Contaminant.Value]
) {
	def +(that: WashSpec): WashSpec = {
		new WashSpec(
			WashIntensity.max(washIntensity, that.washIntensity),
			contamInside ++ that.contamInside,
			contamOutside ++ that.contamOutside
		)
	}
}

class CleanSpec(
	val replacement: Option[TipReplacementPolicy.Value],
	val washIntensity: WashIntensity.Value,
	val contamInside: Set[Contaminant.Value],
	val contamOutside: Set[Contaminant.Value]
)
