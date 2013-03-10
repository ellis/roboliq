package roboliq.entity

case class WashProgram(
	id: String,
	intensity: CleanIntensity.Value,
	contaminantsRemoved: Set[String],
	contaminantsForbidden: Set[String]
) extends Entity