package roboliq.entities

trait WellEvent

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

trait Distribution

case class Distribution_Singular(value: Amount) extends Distribution
case class Distribution_Min(min: Amount) extends Distribution
case class Distribution_Max(max: Amount) extends Distribution
case class Distribution_MinMax(min: Amount, max: Amount) extends Distribution
case class Distribution_Normal(mean: Amount, variance: Amount) extends Distribution
case class Distribution_Uniform(min: Amount, max: Amount) extends Distribution
case class Distribution_Histogram(data: List[(Amount, BigDecimal)]) extends Distribution

case class MixtureDistribution(
	source: Either[Substance, List[MixtureDistribution]],
	distribution: Distribution
)

object MixtureDistribution {
	def empty = new MixtureDistribution(Right(Nil), Distribution_Singular(Amount_Zero()))
}

/**
 * An estimate of amounts of substances in this mixture, as well as the total liquid volume of the mixture.
 */
class Mixture(
	val content: Map[Substance, Amount],
	val volume: LiquidVolume
)

object Mixture {
	def empty = new Mixture(Map(), LiquidVolume.empty)
}

class WellHistory(
	val content0: MixtureDistribution,
	val events: List[WellEvent]
)

class WellState(
	history: WellHistory,
	content: MixtureDistribution,
	estimate: Mixture
) {

}

sealed trait Amount
case class Amount_Zero() extends Amount
case class Amount_Liter(n: BigDecimal) extends Amount
case class Amount_Mol(n: BigDecimal) extends Amount
case class Amount_Gram(n: BigDecimal) extends Amount
