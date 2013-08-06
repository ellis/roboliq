package roboliq.entities

import scalaz._
import Scalaz._
import roboliq.core._

sealed trait Distribution {
	val units: SubstanceUnits.Value
	def isEmpty: Boolean
	def bestGuess: Amount
	/*def add(that: Distribution): RsResult[Distribution]
	
	protected def thatIfMatch(that: Distribution): RsResult[Distribution] = {
		if (this.isEmpty || that.isEmpty || units == that.units)
			RsSuccess(that)
		else
			RsError(s"units don't match: $units vs ${that.units}")
	}*/
}

case class Distribution_Empty() extends Distribution {
	val units = SubstanceUnits.None
	def isEmpty = true
	def bestGuess = Amount.empty
	//def add(that: Distribution): RsResult[Distribution] = RsSuccess(that)
}

case class Distribution_Singular(units: SubstanceUnits.Value, value: BigDecimal) extends Distribution {
	def isEmpty = value == 0
	def bestGuess = Amount(units, value)
	/*def add(that: Distribution): RsResult[Distribution] = thatIfMatch(that).flatMap(_ match {
		case Distribution_Singular(_, value2) => RsSuccess(Distribution_Singular(units, value + value2))
		case Distribution_Uniform(_, min2, max2) => RsSuccess(Distribution_Uniform(units, value + min2, value + max2))
		case Distribution_Normal(_, mean2, variance2) => RsSuccess(Distribution_Normal(units, value + mean2, variance2))
		case Distribution_Histogram(_, data2) => RsSuccess(Distribution_Histogram(units, data2.map(pair => (value + pair._1) -> pair._2)))
		case _ => that.add(this)
	})*/
}

case class Distribution_Uniform(units: SubstanceUnits.Value, min: BigDecimal, max: BigDecimal) extends Distribution {
	def isEmpty = (min == 0 && max == 0)
	def bestGuess = Amount(units, (min + max) / 2)
	/*def add(that: Distribution): RsResult[Distribution] = thatIfMatch(that).flatMap(_ match {
		case Distribution_Uniform(_, min2, max2) => RsSuccess(Distribution_Uniform(units, value + min2, value + max2))
		case Distribution_Normal(_, mean2, variance2) => RsSuccess(Distribution_Normal(units, value + mean2, variance2))
		case Distribution_Histogram(_, data2) => RsSuccess(Distribution_Histogram(units, data2.map(pair => (value + pair._1) -> pair._2)))
		case _ => that.add(this)
	})*/
}
case class Distribution_Normal(units: SubstanceUnits.Value, mean: BigDecimal, variance: BigDecimal) extends Distribution {
	def isEmpty = (mean == 0 && variance == 0)
	def bestGuess = Amount(units, mean)
}

case class Distribution_Histogram(units: SubstanceUnits.Value, data: List[(BigDecimal, BigDecimal)]) extends Distribution {
	def isEmpty = data.isEmpty
	def bestGuess = if (data.isEmpty) Amount.empty else Amount(units, data.head._1)
}
