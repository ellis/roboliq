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
	def apply(mixture: Mixture, volume: LiquidVolume): Aliquot =
		Aliquot(mixture, Distribution.fromVolume(volume))

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
	/**
	 * Example 1:
	 * - 20ul
	 * - 10x
	 * - 10x
	 */
	def fromMixtureAmountList(l: List[(Mixture, Option[AmountSpec])]): RsResult[Mixture] = {
		var volumeExplicit = BigDecimal(0)
		var dilutedNum = BigDecimal(0)
		var dilutedDen = BigDecimal(1)
		var filler_? : Option[Mixture] = None
		for ((mixture, amount_?) <- l) {
			amount_? match {
				case None =>
					if (filler_?.isDefined) return RsError("at most one mixture may be specified without an amount")
					else filler_? = Some(mixture)
				case Some(Amount_l(n)) =>
					volumeExplicit += n
				case Some(Amount_x(num, den)) =>
					// Cross multiple dilutedNum/dilutedDen and 1/n, then sum them together.  Example:
					// 1/10 + 3/20 =>
					//  (1*20+3*10)/(10*20) = 30/200
					dilutedNum = dilutedNum * den + dilutedDen * num
					dilutedDen *= den
			}
		}
		
		def calcMixture(volumeTotal: BigDecimal): RsResult[Mixture] = {
			val volumeDiluted = volumeTotal * dilutedNum / dilutedDen
			val volumeFiller = volumeTotal - volumeExplicit - volumeDiluted
			if (volumeFiller > 0 && filler_?.isEmpty) {
				return RsError("This given list of substances amounts does not add up.  You may wish to provide a diluent solution without an amount specified to act as a filler.")
			}
			else if (volumeFiller < 0) {
				RsError("INTERNAL: fromMixtureAmountList: volumeFiller < 0")
			}
			
			for {
				aliquot_l <- RsResult.mapFirst(l) {
					// TODO: Add a warning if volumeFiller == 0
					case (mixture, None) => RsSuccess(Aliquot(mixture, LiquidVolume.l(volumeFiller)))
					case (mixture, Some(Amount_l(n))) => RsSuccess(Aliquot(mixture, LiquidVolume.l(n)))
					case (mixture, Some(Amount_x(num, den))) => RsSuccess(Aliquot(mixture, LiquidVolume.l(volumeTotal * num / den)))
					case _ => RsError("INTERNAL: fromMixtureAmountList: #1")
				}
			} yield {
				aliquot_l match {
					case List(Aliquot(Mixture(Left(substance)), _)) => Mixture(Left(substance))
					case _ => Mixture(Right(aliquot_l))
				}
			}
		}
	
		(filler_?, volumeExplicit > 0, dilutedNum > 0) match {
			// Dilutions, but no volumes
			case (_, false, true) =>
				calcMixture(1)
			// Explicit volumes, but no dilutions
			case (_, true, false) =>
				calcMixture(volumeExplicit)
			// Explicit volumes and dilutions
			case (_, true, true) =>
				// Example:
				//  volumeExplicit = 4ul
				//  dilutionFraction = 0.6
				//  volumeTotal = 4ul / (1 - 0.6) = 10ul
				//  volumeExplicit / (1 - dilutedNum / dilutedDen) = dilutedDen * volumeExplicit / (dilutedDen - dilutedNum)
				calcMixture(dilutedDen * volumeExplicit / (dilutedDen - dilutedNum))
			// Nothing
			case (None, false, false) =>
				RsSuccess(Mixture.empty)
			// Only a substance without amount
			case (Some(mixture), false, false) =>
				RsSuccess(mixture)
		}
	}
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
		}).mkString("(", "+", ")") + "@" + volume.toString
	}
}

object AliquotFlat {
	def empty = new AliquotFlat(Map())

	def apply(aliquot: Aliquot): AliquotFlat = {
		val content0: Map[Substance, Amount] = aliquot.mixture.source match {
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
		// FIXME: need to also handle other units besides just liters
		val amountContent0 = content0.map(_._2 match { case Amount(SubstanceUnits.Liter, n) => n }).sum
		val amountFraction = aliquot.distribution.bestGuess.amount / amountContent0 
		val content1 = content0.map { case (substance, amount0) => (substance, amount0.copy(amount = amount0.amount * amountFraction)) }
		new AliquotFlat(content1)
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

sealed trait AmountSpec
case class Amount_l(n: BigDecimal) extends AmountSpec
//case class Amount_mol(n: BigDecimal) extends AmountSpec
//case class Amount_g(n: BigDecimal) extends AmountSpec
case class Amount_x(num: BigDecimal, den: BigDecimal) extends AmountSpec
