package roboliq.evoware.translator

import spray.json.JsValue

case class EvowareFacts(
	factsEquipment: String,
	factsVariable: String,
	factsValue_? : Option[String]
)

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

case class PipetterWashProgram(
	wasteGrid: Int,
	wasteSite: Int,
	cleanerGrid: Int,
	cleanerSite: Int,
	wasteVolume: Int,
	wasteDelay: Int,
	cleanerVolume: Int,
	cleanerDelay: Int,
	airgapVolume: Int,
	airgapSpeed: Int,
	retractSpeed: Int,
	fastWash: Boolean
)

case class PipetterWashTips(
	equipment: String,
	program: JsValue,
	syringes: List[Int]
)

case class TransporterMovePlate(
	equipment: String,
	program_? : Option[String],
	`object`: String,
	destination: String,
	evowareMoveBackToHome_? : Option[Boolean]
	//evowareLidHandling_? : Option[LidHandling.Value]
)
