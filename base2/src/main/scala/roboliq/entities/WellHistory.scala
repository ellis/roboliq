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

case class Distribution_Unknown() extends Distribution
case class Distribution_Normal(mean: SubstanceAmount, variance: SubstanceAmount) extends Distribution
case class Distribution_Uniform(min: SubstanceAmount, max: SubstanceAmount) extends Distribution
case class Distribution_Histogram() extends Distribution

class Mixture(
	contents: List[(Substance, Distribution)]
)

class WellHistory(
	state0: WellState,
	events: List[WellEvent]
) {

}
