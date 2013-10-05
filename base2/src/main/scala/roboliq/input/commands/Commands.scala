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