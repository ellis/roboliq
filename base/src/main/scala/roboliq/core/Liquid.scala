package roboliq.core

import scalaz._
import Scalaz._
import roboliq.utils.MathUtils


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


sealed class Liquid private(
	val contents: Map[Substance, BigDecimal]
) {
	val id = {
		if (contents.isEmpty) "<EMPTY>"
		else contents.map(pair => "\"" + pair._1.id + "\"@" + MathUtils.toChemistString3(pair._2)).mkString("(", ",", ")")
	}
	val substance_l = contents.keys.toList
	
	/** Tip cleaning policy when handling this substance with pipetter. */
	val tipCleanPolicy: TipCleanPolicy = substance_l.map(_.tipCleanPolicy).concatenate
	/** List of contaminants in this substance */
	val contaminants: Set[String] = substance_l.map(_.contaminants).concatenate
	/** Value per unit (either liter or mol) of the substance (this can be on a different scale than costPerUnit) */
	val valuePerUnit_? : Option[BigDecimal] = substance_l.map(_.valuePerUnit_?).concatenate
	
//	val gramPerMole_? : Option[BigDecimal] = substance_l.map(_.gramPerMole_?).concatenate
//	val celciusToLiterPerMole: List[CelciusToLiterPerMole]
//	val celciusAndConcToLiterPerMole: List[CelciusAndConcToLiterPerMole]
//	val celciusToViscosity: List[CelciusToViscosity]
	
	val isEmpty: Boolean = contents.isEmpty
	val isLiquid: Boolean = substance_l.exists(_.isLiquid)
	// "water"@5.44e-10,"oil"@1.23e-23,
	
	override def toString = id
	override def equals(that: Any): Boolean = that match {
		case that_# : Liquid => id == that_#.id
		case _ => assert(false); false
	}
	override def hashCode() = id.hashCode()
}

object Liquid {
	def apply(contents: Map[Substance, BigDecimal]): Liquid = {
		val l = contents.toList.sortBy(_._2).reverse
		// Make sure fractions are normalized to 1
		val factor = 1 / contents.values.sum
		val contents_# = contents.mapValues(_ * factor)
		new Liquid(contents_#)
	}
	
	val Empty = new Liquid(Map())
}
