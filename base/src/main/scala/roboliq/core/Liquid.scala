package roboliq.core

import scalaz._
import Scalaz._


/**
 * Enumeration of possible contaminants on a tip; this probably won't be used now, so consider deleting it.
 */
object Contaminant extends Enumeration {
	val Cell, DNA, DSMO, Decon, Other = Value
}

/*
A = none?, light*, thorough?, decon (Same Group, different well)
B = thorough*, decon (different group)
C = thorough*, decon (before next group)

NoneThorough?
NoneDecon?
LightThorough*
LightDecon
-Thorough (because this would be the behavior for non-grouped liquids)
ThoroughDecon?
Decon


Decon before entering from another group (boolean) (default is thorough)
Decon before entering the next group (boolean) (default is thorough)
degree when entering from another well of the same group

Thorough-None-Thorough
Thorough-Light-Thorough*
-Thorough-Thorough-Thorough
-Thorough-Decon-Thorough
Decon-*-Thorough?
Decon-None-Decon?
Decon-Light-Decon?
Decon-Thorough-Decon?
Decon-Decon-Decon


*/

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
		def append(a: TipCleanPolicy, b: TipCleanPolicy) = max(a, b)
		def zero = NN
	}
}

/**
 * Represents a liquid.
 * This is one of the most-used classes for our pipetting routines.
 * 
 * @note Liquid should be better integrated with the newer Substance and VesselContent classes.
 * For example, Liquid could maintain a list of Substances and their ratios and concentrations,
 * more similar to VesselContent.
 * 
 * @see [[roboliq.core.Substance]]
 * @see [[roboliq.core.SubstanceLiquid]]
 * @see [[roboliq.core.VesselContent]]
 */
class Liquid(
	val id: String,
	val sName_? : Option[String],
	val sFamily: String,
	val contaminants: Set[Contaminant.Value],
	val tipCleanPolicy: TipCleanPolicy,
	val multipipetteThreshold: BigDecimal
) {
	def +(other: Liquid): Liquid = {
		if (this eq other)
			this
		else if (this eq Liquid.empty)
			other
		else if (other eq Liquid.empty)
			this
		else {
			assert(id != other.id)
			val id_# = id+":"+other.id
			val tipCleanPolicy_# = TipCleanPolicy.max(tipCleanPolicy, other.tipCleanPolicy)
			new Liquid(
				id_#,
				None,
				sFamily,
				contaminants ++ other.contaminants,
				tipCleanPolicy_#,
				if (multipipetteThreshold <= other.multipipetteThreshold) multipipetteThreshold else other.multipipetteThreshold
			)
		}
	}
	
	def getName() = if (id == null) "<unnamed>" else id
	
	override def toString = getName()
	override def equals(that: Any): Boolean = {
		that match {
			case b: Liquid => id == b.id
			case _ => assert(false); false
		}
	}
	override def hashCode() = id.hashCode()
}

object Liquid {
	/** Empty liquid */
	val empty = new Liquid("<EMPTY>", None, "", Set(), TipCleanPolicy.Thorough, 0.0)
}
