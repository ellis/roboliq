package roboliq.entities

import scala.collection.mutable.ArrayBuffer
import scalaz.Semigroup
import scalaz.Monoid


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
		if (policy.contains("Air") || policy.contains("_A_")) PipettePosition.Free
		else if (policy.contains("Dry")) PipettePosition.DryContact
		else PipettePosition.WetContact
	}
}

//case class PipetteSpec(sName: String, aspirate: PipettePosition.Value, dispense: PipettePosition.Value, mix: PipettePosition.Value)

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

/**
 * Represents the intensity of tip cleaning required by a given liquid.
 * 
 * @param enter intensity with which tip must have been washed prior to entering a liquid.
 * @param within intensity with which tip must be washed between pipetteing operations performed in the same [[robolq.core.LiquidGroup]].
 * @param exit intensity with which tip must be washed after entering a liquid. 
 */
case class TipCleanPolicy(
	val enter: CleanIntensity.Value,
	val exit: CleanIntensity.Value
)
/**
 * Contains the standard TipCleanPolicy instantiations.
 */
object TipCleanPolicy {
	val NN = new TipCleanPolicy(CleanIntensity.None, CleanIntensity.None)
	val TN = new TipCleanPolicy(CleanIntensity.Thorough, CleanIntensity.None)
	val TL = new TipCleanPolicy(CleanIntensity.Thorough, CleanIntensity.Light)
	val TT = new TipCleanPolicy(CleanIntensity.Thorough, CleanIntensity.Thorough)
	val DD = new TipCleanPolicy(CleanIntensity.Decontaminate, CleanIntensity.Decontaminate)
	val ThoroughNone = TN
	val Thorough = TT
	val Decontaminate = DD
	
	/**
	 * Return a new policy that takes the maximum sub-intensities of `a` and `b`.
	 */
	def max(a: TipCleanPolicy, b: TipCleanPolicy): TipCleanPolicy = {
		new TipCleanPolicy(
			CleanIntensity.max(a.enter, b.enter),
			CleanIntensity.max(a.exit, b.exit)
		)
	}
	
	implicit object TipCleanPolicyMonoid extends Monoid[TipCleanPolicy] {
		def append(a: TipCleanPolicy, b: => TipCleanPolicy) = max(a, b)
		def zero = NN
	}
}

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
