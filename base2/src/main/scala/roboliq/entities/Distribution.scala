package roboliq.entities

import scalaz._
import Scalaz._
import roboliq.core._
import ch.ethz.reactivesim.RsError

sealed trait Distribution {
	val units: SubstanceUnits.Value
	def isEmpty: Boolean
	def bestGuess: Amount
	
	def toVolume: RsResult[LiquidVolume] = {
		for {
			_ <- RsResult.assert(units == SubstanceUnits.Liter, "cannot convert unit `$units` to a liquid volume")
		} yield {
			LiquidVolume.l(bestGuess.amount)
		}
	}

	def add(that: Distribution): RsResult[Distribution] = {
		if (this.isEmpty) RsSuccess(that)
		else if (that.isEmpty) RsSuccess(this)
		else if (units != that.units) RsError(s"units don't match: $units vs ${that.units}")
		else {
			(this, that) match {
				case (a: Distribution_Sum, b: Distribution_Sum) => RsSuccess(Distribution_Sum(units, a.summands ++ b.summands))
				case (a: Distribution_Sum, b) => RsSuccess(Distribution_Sum(units, a.summands ++ List(b)))
				case (a, b: Distribution_Sum) => RsSuccess(Distribution_Sum(units, a :: b.summands))
				//case (a: Distribution_Singular, b) => addSingular(a.value, b)
				case (a, b) => RsSuccess(Distribution_Sum(units, List(a, b)))
			}
		}
	}
	
	def subtract(that: Distribution): RsResult[Distribution] =
		add(that.negate)
	
	def negate(): Distribution = {
		this match {
			case Distribution_Empty() => this
			case Distribution_Singular(_, value) => Distribution_Singular(units, -value)
			case Distribution_Uniform(_, min, max) => Distribution_Uniform(units, -max, -min)
			case Distribution_Normal(_, mean, variance) => Distribution_Normal(units, -mean, variance)
			case Distribution_Histogram(_, data) => Distribution_Histogram(units, data.map(pair => (-pair._1, pair._2)))
			case Distribution_Sum(_, summands) => Distribution_Sum(units, summands.map(_.negate))
		}
	}
	
	protected def thatIfMatch(that: Distribution): RsResult[Distribution] = {
		if (this.isEmpty || that.isEmpty || units == that.units)
			RsSuccess(that)
		else
			RsError(s"units don't match: $units vs ${that.units}")
	}
	
	/*
	private def addSingular(value: BigDecimal, that: Distribution): RsResult[Distribution] = that match {
		case Distribution_Empty() => RsSuccess(this)
		case Distribution_Singular(_, value2) => RsSuccess(Distribution_Singular(units, value + value2))
		case Distribution_Uniform(_, min2, max2) => RsSuccess(Distribution_Uniform(units, value + min2, value + max2))
		case Distribution_Normal(_, mean2, variance2) => RsSuccess(Distribution_Normal(units, value + mean2, variance2))
		case Distribution_Histogram(_, data2) => RsSuccess(Distribution_Histogram(units, data2.map(pair => (value + pair._1) -> pair._2)))
		case Distribution_Sum(_, summands) => RsSuccess(Distribution_Sum(units, summands ++ List(this)))
	}*/
}

object Distribution {
	def fromVolume(volume: LiquidVolume) = Distribution_Singular(SubstanceUnits.Liter, volume.l)
}

case class Distribution_Empty() extends Distribution {
	val units = SubstanceUnits.None
	def isEmpty = true
	def bestGuess = Amount.empty
}

case class Distribution_Singular(units: SubstanceUnits.Value, value: BigDecimal) extends Distribution {
	def isEmpty = value == 0
	def bestGuess = Amount(units, value)
}

case class Distribution_Uniform(units: SubstanceUnits.Value, min: BigDecimal, max: BigDecimal) extends Distribution {
	def isEmpty = (min == 0 && max == 0)
	def bestGuess = Amount(units, (min + max) / 2)
}
case class Distribution_Normal(units: SubstanceUnits.Value, mean: BigDecimal, variance: BigDecimal) extends Distribution {
	def isEmpty = (mean == 0 && variance == 0)
	def bestGuess = Amount(units, mean)
}

case class Distribution_Histogram(units: SubstanceUnits.Value, data: List[(BigDecimal, BigDecimal)]) extends Distribution {
	def isEmpty = data.isEmpty
	def bestGuess = if (data.isEmpty) Amount.empty else Amount(units, data.head._1)
}

case class Distribution_Sum(units: SubstanceUnits.Value, summands: List[Distribution]) extends Distribution {
	def isEmpty = summands.isEmpty
	def bestGuess = if (summands.isEmpty) Amount.empty else Amount(units, summands.filter(_.units == units).foldLeft(BigDecimal(0)){(acc, dist) => acc + dist.bestGuess.amount})
}
