package roboliq.entities

import scalaz._
import Scalaz._

sealed trait WellEvent

case class WellEvent_TipDetectVolume(
	time: java.util.Date,
	tipIndex: Int,
	tipModel: String,
	site: String,
	labwareModel: String,
	row: Int,
	col: Int,
	measurement: BigDecimal
) extends WellEvent

case class WellEvent_Aspriate(
	time: java.util.Date,
	spec: String,
	tipIndex: Int,
	tipModel: String,
	site: String,
	labwareModel: String,
	row: Int,
	col: Int,
	volume: LiquidVolume
) extends WellEvent

case class WellEvent_Dispense(
	time: java.util.Date,
	spec: String,
	tipIndex: Int,
	tipModel: String,
	tipVolume0: LiquidVolume,
	tipVolume: LiquidVolume,
	site: String,
	labwareModel: String,
	row: Int,
	col: Int,
	multipipetteStep: Int,
	volume: LiquidVolume
) extends WellEvent

/**
 * An aliquot is an amount of a mixture.
 * A mixture tells us the ratios of the contained substances:
 *  it is composed of aliquots of substances, where the absolute relative ratios are important rather than the absolute values. 
 */

case class Aliquot(
	mixture: Mixture,
	distrubtion: Distribution
) {
	/*
	def flatten: AliquotFlat =
		AliquotFlat(
		)
	*/
}

object Aliquot {
	def empty = new Aliquot(Mixture.empty, Distribution_Empty())
}

case class Mixture(
	source: Either[Substance, List[Aliquot]]
) {
	/*def flatten: Map[Substance, Amount] = {
		source match {
			case Left(substance) => substance :: Nil
			case Right(aliquot_l) => aliquot_l.flatMap(_.mixture.flatten)
		}
	}*/
	/** Tip cleaning policy when handling this substance with pipetter. */
	val tipCleanPolicy: TipCleanPolicy = source match {
		case Left(substance) => substance.tipCleanPolicy
		case Right(aliquot_l) => aliquot_l.map(_.mixture.tipCleanPolicy).foldLeft(TipCleanPolicy.NN)(TipCleanPolicy.max(_, _))
	}
	/** List of contaminants in this substance */
	val contaminants: Set[String] = source match {
		case Left(substance) => substance.contaminants
		case Right(aliquot_l) => aliquot_l.flatMap(_.mixture.contaminants).toSet
	}
}

object Mixture {
	def empty = new Mixture(Right(Nil))
}

/**
 * An estimate of amounts of substances in this mixture, as well as the total liquid volume of the mixture.
 */
class AliquotFlat(
	val content: Map[Substance, Amount],
	val volume: LiquidVolume
) {
	val substance_l = content.keys.toList
	
	/** Tip cleaning policy when handling this substance with pipetter. */
	val tipCleanPolicy: TipCleanPolicy = substance_l.map(_.tipCleanPolicy).concatenate
	/** List of contaminants in this substance */
	val contaminants: Set[String] = substance_l.map(_.contaminants).concatenate
	/** Value per unit (either liter or mol) of the substance (this can be on a different scale than costPerUnit) */
	val valuePerUnit_? : Option[BigDecimal] = substance_l.map(_.valuePerUnit_?).concatenate

}

object AliquotFlat {
	def empty = new AliquotFlat(Map(), LiquidVolume.empty)
}

class WellHistory(
	val aliquot0: Aliquot,
	val events: List[WellEvent]
)

object SubstanceUnits extends Enumeration {
	val None, Liter, Mol, Gram = Value
}

case class Amount(units: SubstanceUnits.Value, amount: BigDecimal)
object Amount {
	def empty = Amount(SubstanceUnits.None, 0)
}
