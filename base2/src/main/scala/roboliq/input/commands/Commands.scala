package roboliq.input.commands

import roboliq.core._
import roboliq.entities._

trait Command

case class Distribute(
	source: String,
	destination: String,
	volume: LiquidVolume,
	dispensePosition_? : Option[String],
	tipHandling_? : Option[String],
	tipHandlingBefore_? : Option[String],
	tipHandlingBetween_? : Option[String],
	tipHandlingAfter_? : Option[String],
	tipModel_? : Option[String],
	pipettePolicy_? : Option[String]
) extends Command