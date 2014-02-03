package roboliq.input.commands

import roboliq.core._
import roboliq.entities._

trait Command

case class Distribute(
	source: String,
	destination: String,
	volume: LiquidVolume,
	contact_? : Option[PipettePosition.Value],
	sterilize_? : Option[CleanIntensity.Value],
	sterilizeBefore_? : Option[CleanIntensity.Value],
	sterilizeBetween_? : Option[CleanIntensity.Value],
	sterilizeAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[String],
	pipettePolicy_? : Option[String]
) extends Command

sealed trait TitrationAmount
case class TitrationAmount_Volume(volume: LiquidVolume) extends TitrationAmount
case class TitrationAmount_Range(min: LiquidVolume, max: LiquidVolume) extends TitrationAmount

// REFACTOR: delete this
case class TitrationSource(
	source: String,
	amount_? : Option[TitrationAmount]
)

case class TitrationSeries(
	steps: List[TitrationStep],
	destination: PipetteDestinations,
	volume_? : Option[LiquidVolume]
) extends Command

case class TitrationStep(
	source: PipetteSources,
	volume_? : Option[List[LiquidVolume]],
	min_? : Option[LiquidVolume],
	max_? : Option[LiquidVolume],
	contact_? : Option[PipettePosition.Value],
	sterilize_? : Option[CleanIntensity.Value],
	sterilizeBefore_? : Option[CleanIntensity.Value],
	sterilizeBetween_? : Option[CleanIntensity.Value],
	sterilizeAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[String],
	pipettePolicy_? : Option[String]
) extends Command