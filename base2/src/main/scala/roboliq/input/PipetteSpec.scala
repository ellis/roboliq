package roboliq.input

import roboliq.core._
import roboliq.entities._


case class PipetteSpec(
	source_l: List[(Labware, RowCol)],
	destination_l: List[(Labware, RowCol)],
	volume: LiquidVolume,
	pipettePolicy_? : Option[String],
	preClean_? : Option[CleanIntensity.Value]
)

case class PipetteSpecList(
	step_l: List[PipetteSpec]
)