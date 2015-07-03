package roboliq.evoware.translator

case class SealerRun(
	equipment: String,
	program: String,
	`object`: String
)

case class TransporterMovePlate(
	equipment: String,
	program_? : Option[String],
	`object`: String,
	destination: String,
	evowareMoveBackToHome_? : Option[Boolean]
	//evowareLidHandling_? : Option[LidHandling.Value]
)
