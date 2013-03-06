package roboliq.core

case class WashProgram(
	id: String,
	intensity: CleanIntensity.Value,
	contaminantsRemoved: Set[String],
	contaminantsForbidden: Set[String]
)