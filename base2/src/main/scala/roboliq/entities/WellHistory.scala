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

class WellHistory(
	state0: WellState,
	events: List[WellEvent]
) {

}
