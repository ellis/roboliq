package roboliq.input

import roboliq.core._
import roboliq.entities._


case class PipetteSpec(
	source_l: List[(Labware, RowCol)],
	destination_l: List[(Labware, RowCol)],
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