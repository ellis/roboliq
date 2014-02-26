package roboliq.input.commands

import roboliq.core._
import roboliq.entities._


case class PipetteSpec(
	sources: PipetteSources,
	destinations: PipetteDestinations,
	volume_l: List[LiquidVolume],
	pipettePolicy_? : Option[String],
	sterilize_? : Option[CleanIntensity.Value],
	cleanBefore_? : Option[CleanIntensity.Value],
	sterilizeBetween_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[TipModel]
)

case class PipetteSpecList(
	step_l: List[PipetteSpec]
)