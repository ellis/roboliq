package roboliq.core

import scala.collection.mutable.ArrayBuffer

import scalaz.Semigroup


abstract class LiquidPropertiesFamily
object LiquidPropertiesFamily {
	case object Water extends LiquidPropertiesFamily
	case object Cells extends LiquidPropertiesFamily
	case object DMSO extends LiquidPropertiesFamily
	case object Decon extends LiquidPropertiesFamily
}

/**
 * Enumeration of intensities with which the tips should be washed.
 */
object WashIntensity extends Enumeration {
	val None, Light, Thorough, Decontaminate = Value
	
	def max(a: WashIntensity.Value, b: WashIntensity.Value): WashIntensity.Value = {
		if (a >= b) a else b
	}
	
	def max(l: Traversable[WashIntensity.Value]): WashIntensity.Value = {
		l.foldLeft(WashIntensity.None)((acc, intensity) => max(acc, intensity))
	}
}

/** Represents a volume of liquid. */
class LiquidVolume private (val _nl: Int) {
	/** Volume in nanoliters [nl] */
	def nl: BigDecimal = _nl
	/** Volume in microliters [ul] */
	def ul: BigDecimal = (nl / 1000)
	/** Volume in milliliters [ml] */
	def ml: BigDecimal = (nl / 1000000)
	/** Volume in liters [l] */
	def l: BigDecimal = (nl / 1000000000)
	
	def -(that: LiquidVolume): LiquidVolume = new LiquidVolume(_nl - that._nl)
	def +(that: LiquidVolume): LiquidVolume = new LiquidVolume(_nl + that._nl)
	def *(n: BigDecimal): LiquidVolume = new LiquidVolume((n * _nl).toInt)
	def /(n: BigDecimal): LiquidVolume = new LiquidVolume((BigDecimal(_nl) / n).toInt)
	
	/** Return true if the volume is 0. */
	def isEmpty: Boolean = (_nl == 0)
	def <(that: LiquidVolume): Boolean = (_nl < that._nl)
	def <=(that: LiquidVolume): Boolean = (_nl <= that._nl)
	def >(that: LiquidVolume): Boolean = (_nl > that._nl)
	def >=(that: LiquidVolume): Boolean = (_nl >= that._nl)
	def ==(that: LiquidVolume): Boolean = (_nl == that._nl)
	def !=(that: LiquidVolume): Boolean = (_nl != that._nl)
	
	override def equals(that: Any): Boolean = {
		assert(that.isInstanceOf[LiquidVolume])
		_nl == that.asInstanceOf[LiquidVolume]._nl
	}
	override def hashCode = _nl.hashCode()
	
	override def toString = {
		if (_nl >= 1000000)
			(nl / 1000000).toString + " ml"
		else if (_nl >= 1000)
			(nl / 1000).toString + " ul"
		else
			nl.toString + " nl"
	}
}

/** Factory for [[roboliq.protocol.LiquidVolume]] */
object LiquidVolume {// extends Semigroup[LiquidVolume] {
	def nl(n: Int): LiquidVolume = new LiquidVolume(n)
	def nl(n: BigDecimal): LiquidVolume = new LiquidVolume(n.toInt)
	def ul(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000).toInt)
	def ml(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000000).toInt)
	def l(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000000000).toInt)
	
	val empty: LiquidVolume = new LiquidVolume(0)
	
	def max(a: LiquidVolume, b: LiquidVolume): LiquidVolume = {
		if (a._nl > b._nl) a else b
	}
	
	def min(a: LiquidVolume, b: LiquidVolume): LiquidVolume = {
		if (a._nl < b._nl) a else b
	}
	
	//def append(a: LiquidVolume, b: => LiquidVolume): LiquidVolume = a + b
}

/**
 * Enumeration for where the tips should be positioned with respect to the liquid being pipetted.
 * `Free` means in the air, `WetContact` means just below the liquid surface,
 * `DryContact` means just above the bottom of an empty vessel.
 */
object PipettePosition extends Enumeration {
	val Free, WetContact, DryContact = Value
	
	def getPositionFromPolicyNameHack(policy: String): PipettePosition.Value = {
		if (policy.contains("Air")) PipettePosition.Free
		else if (policy.contains("Dry")) PipettePosition.DryContact
		else PipettePosition.WetContact
	}
}

//case class PipetteSpec(sName: String, aspirate: PipettePosition.Value, dispense: PipettePosition.Value, mix: PipettePosition.Value)

/** Basically a tuple of a pipette policy name and the position of the tips while pipetting. */
case class PipettePolicy(id: String, pos: PipettePosition.Value)

object PipettePolicy {
	def fromName(name: String): PipettePolicy = {
		val pos = {
			if (name.contains("Air")) PipettePosition.Free
			else if (name.contains("Dry")) PipettePosition.DryContact
			else PipettePosition.WetContact
		}
		PipettePolicy(name, pos)
	}
}

/**
 * An enumeration of methods for replacing tips.
 */
object TipReplacementPolicy extends Enumeration { // FIXME: Replace this with TipReplacementPolicy following Roboease
	val ReplaceAlways, KeepBetween, KeepAlways = Value
}

case class TipHandlingOverrides(
	val replacement_? : Option[TipReplacementPolicy.Value],
	val washIntensity_? : Option[WashIntensity.Value],
	val allowMultipipette_? : Option[Boolean],
	val contamInside_? : Option[Set[Contaminant.Value]],
	val contamOutside_? : Option[Set[Contaminant.Value]]
)

object TipHandlingOverrides {
	def apply() = new TipHandlingOverrides(None, None, None, None, None)
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

/**
 * Specification for cleaning of tips either by replacement or washing.
 * @param replacement optional replacement method.
 * @param washIntensity intensity with which tips should be washed.
 */
class CleanSpec(
	val replacement: Option[TipReplacementPolicy.Value],
	val washIntensity: WashIntensity.Value,
	val contamInside: Set[Contaminant.Value],
	val contamOutside: Set[Contaminant.Value]
)
