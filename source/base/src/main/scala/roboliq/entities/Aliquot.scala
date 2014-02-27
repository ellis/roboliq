package roboliq.entities

import scalaz._
import Scalaz._
import roboliq.core._

/**
 * An aliquot is an amount of a mixture.
 * A mixture tells us the ratios of the contained substances:
 *  it is composed of aliquots of substances, where the absolute relative ratios are important rather than the absolute values. 
 */

case class Aliquot(
	mixture: Mixture,
	distribution: Distribution
) {
	def add(that: Aliquot): RsResult[Aliquot] = {
		for {
			distribution_~ <- distribution.add(that.distribution)
		} yield {
			(this.distribution.isEmpty, that.distribution.isEmpty) match {
				case (true, _) => that
				case (_, true) => this
				case _ => Aliquot(Mixture(Right(List(this, that))), distribution_~)
			}
		}
	}
	
	def remove(amount: Distribution): RsResult[Aliquot] = {
		for {
			amount_~ <-  distribution.subtract(amount)
		} yield {
			Aliquot(mixture, amount_~)
		}
	}
	
	def toShortString: String = {
		if (distribution.isEmpty) "EMPTY"
		else {
			mixture.toShortString+"@"+distribution.toShortString
		}
	}
	
	override def toString = toShortString
}

object Aliquot {
	def empty = new Aliquot(Mixture.empty, Distribution_Empty())
}

case class Mixture(
	source: Either[Substance, List[Aliquot]]
) {
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
	
	def toShortString: String = {
		source match {
			case Left(substance) => substance.label.getOrElse(substance.key)
			case Right(Nil) => "EMPTY"
			case Right(aliquot_l) => aliquot_l.map(_.toShortString).mkString("(", "+", ")")
		}
	}
}

object Mixture {
	def empty = new Mixture(Right(Nil))
}

/**
 * An estimate of amounts of substances in this mixture, as well as the total liquid volume of the mixture.
 */
class AliquotFlat(
	val content: Map[Substance, Amount]
) {
	val volume = LiquidVolume.l(content.values.filter(_.units == SubstanceUnits.Liter).map(_.amount).sum)
	val substance_l = content.keys.toList
	
	/** Tip cleaning policy when handling this substance with pipetter. */
	val tipCleanPolicy: TipCleanPolicy = substance_l.map(_.tipCleanPolicy).concatenate
	/** List of contaminants in this substance */
	val contaminants: Set[String] = substance_l.map(_.contaminants).concatenate
	/** Value per unit (either liter or mol) of the substance (this can be on a different scale than costPerUnit) */
	val valuePerUnit_? : Option[BigDecimal] = substance_l.map(_.valuePerUnit_?).concatenate

	def toMixtureString: String = {
		content.toList match {
			case Nil => "EMPTY"
			case (substance, amount) :: Nil =>
				substance.label.getOrElse(substance.key)
			case content_l =>
				content_l.map(pair => {
					val (substance, amount) = pair
					s"${substance.label.getOrElse(substance.key)}@${amount.amount.toString}${SubstanceUnits.toShortString(amount.units)}"
				}).mkString("+")
		}
	}

	override def toString: String = {
		content.toList.map(pair => {
			val (substance, amount) = pair
			s"${substance.label.getOrElse(substance.key)}@${amount.amount.toString}${SubstanceUnits.toShortString(amount.units)}"
		}).mkString("+") + "@" + volume.toString
	}
}

object AliquotFlat {
	def empty = new AliquotFlat(Map())

	def apply(aliquot: Aliquot): AliquotFlat = {
		val content: Map[Substance, Amount] = aliquot.mixture.source match {
			case Left(substance) => Map(substance -> aliquot.distribution.bestGuess)
			case Right(aliquot_l) =>
				val flat_l = aliquot_l.map(AliquotFlat.apply)
				val content_l = flat_l.flatMap(_.content.toList)
				val m = content_l.groupBy(_._1)
				m.map(pair => {
					val (substance, content_l) = pair
					val amount_l = content_l.map(_._2)
					val amount = Amount(
						units = amount_l.head.units,
						amount = amount_l.map(_.amount).sum
					)
					substance -> amount
				})
		}
		new AliquotFlat(content)
	}
}

class WellHistory(
	val aliquot0: Aliquot,
	val events: List[WellEvent]
)

object SubstanceUnits extends Enumeration {
	val None, Liter, Mol, Gram = Value
	
	def toShortString(units: Value): String = units match {
		case None => ""
		case Liter => "l"
		case Mol => "mol"
		case Gram => "g"
	}
}

case class Amount(units: SubstanceUnits.Value, amount: BigDecimal)
object Amount {
	def empty = Amount(SubstanceUnits.None, 0)
}