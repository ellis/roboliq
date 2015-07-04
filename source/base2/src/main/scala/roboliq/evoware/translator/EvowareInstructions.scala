package roboliq.evoware.translator

case class PipetterItem(
	syringe: Int,
	well: String,
	volume: String
)

case class PipetterSpirate(
	equipment: String,
	program: String,
	items: List[PipetterItem]
)

case class PipetterItem2(
	syringe: Int,
	source: String,
	destination: String,
	volume: String
)

case class PipetterSpirate2(
	equipment: String,
	program: String,
	items: List[PipetterItem2]
)

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
