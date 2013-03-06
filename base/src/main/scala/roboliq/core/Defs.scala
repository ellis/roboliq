package roboliq.core

import scala.collection.mutable.ArrayBuffer

import scalaz.Semigroup


/**
 * Base class for case objects used to specify a family of liquid properties.
 * This indicates how it will need to be handled during pipetting.
 */
abstract class LiquidPropertiesFamily
/**
 * Holds the standard LiquidPropertiesFamily case objects: `Water`, `Cells`, `DMSO`, and `Decon`.
 */
object LiquidPropertiesFamily {
	/** Liquid is water-like. */
	case object Water extends LiquidPropertiesFamily
	/** Liquid contains cells. */
	case object Cells extends LiquidPropertiesFamily
	case object DMSO extends LiquidPropertiesFamily
	case object Decon extends LiquidPropertiesFamily
}

/**
 * Enumeration of intensities with which the tips should be washed.
 */
object CleanIntensity extends Enumeration {
	val None, Flush, Light, Thorough, Decontaminate = Value
	
	/** Return the more intense wash value. */
	def max(a: CleanIntensity.Value, b: CleanIntensity.Value): CleanIntensity.Value = {
		if (a >= b) a else b
	}
	
	/** Return the most intense wash value in `l`. */
	def max(l: Traversable[CleanIntensity.Value]): CleanIntensity.Value = {
		l.foldLeft(CleanIntensity.None)((acc, intensity) => max(acc, intensity))
	}
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
case class PipettePolicy(id: String, pos: PipettePosition.Value) {
	override def equals(that: Any): Boolean = {
		that match {
			case b: PipettePolicy => id == b.id
			case _ => false
		}
	}
}

object PipettePolicy {
	def fromName(name: String): PipettePolicy = {
		val pos = PipettePosition.getPositionFromPolicyNameHack(name)
		PipettePolicy(name, pos)
	}
}

/**
 * An enumeration of methods for replacing tips.
 */
object TipReplacementPolicy extends Enumeration { // FIXME: Replace this with TipReplacementPolicy following Roboease
	val ReplaceAlways, KeepBetween, KeepAlways = Value
}

/**
 * Allows the user to override the default tip-handling procedures during pipetting.
 * @param replacement_? optional TipReplacementPolicy.
 * @param washIntensity_? optional CleanIntensity.
 * @param allowMultipipette_? optional boolean value, false prevents multi-pipetting (i.e. aspirating once and dispensing multiple times)
 */
case class TipHandlingOverrides(
	val replacement_? : Option[TipReplacementPolicy.Value],
	val washIntensity_? : Option[CleanIntensity.Value],
	val allowMultipipette_? : Option[Boolean],
	val contamInside_? : Option[Set[String]],
	val contamOutside_? : Option[Set[String]]
)

/**
 * Provides a TipHandlingOverrides object with no overrides.
 */
object TipHandlingOverrides {
	def apply() = new TipHandlingOverrides(None, None, None, None, None)
}

/**
 * Basically now just a wrapper around [[roboliq.core.CleanIntensity]],
 * since I'm probably not going to use the contamination values anymore;
 * I should probably get rid of this class.
 */
class WashSpec(
	val washIntensity: CleanIntensity.Value,
	val contamInside: Set[String],
	val contamOutside: Set[String]
) {
	def +(that: WashSpec): WashSpec = {
		new WashSpec(
			CleanIntensity.max(washIntensity, that.washIntensity),
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
	val washIntensity: CleanIntensity.Value,
	val contamInside: Set[String],
	val contamOutside: Set[String]
)

trait CmdToken
trait Event