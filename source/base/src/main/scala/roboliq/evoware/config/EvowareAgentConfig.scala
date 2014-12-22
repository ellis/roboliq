package roboliq.evoware.config

/**
 * @param evowareDir Evoware data directory
 * @param labwareModels Labware that this robot can use
 * @param tipModels Tip models that this robot can use
 * @param tips This robot's tips
 * @param tableSetups Table setups for this robot
 * @param transporterBlacklist list of source/vector/destination combinations which should not be used when moving labware with robotic transporters
 * @param sealerPrograms Sealer programs
 */
case class EvowareAgentConfig(
	evowareDir: String,
	labwareModels: Map[String, LabwareModelConfig],
	tipModels: Map[String, TipModelConfig],
	tips: List[TipConfig],
	tableSetups: Map[String, TableSetupConfig],
	transporterBlacklist: List[TransporterBlacklistConfig],
	sealerPrograms: Map[String, SealerProgramConfig]
)

case class LabwareModelConfig(
	label_? : Option[String],
	evowareName: String
)

case class TipModelConfig(
	min: java.lang.Double,
	max: java.lang.Double
)

case class TipConfig(
	row: Integer,
	permanentModel_? : Option[String],
	models: List[String]
)

/**
 * @param tableFile Path to evoware table file
 * @param sites Site definitions
 * @param pipetterSites List of sites the pipetter can access
 * @param userSites List of sites the user can directly access
 */
case class TableSetupConfig(
	tableFile: String,
	sites: Map[String, SiteConfig],
	pipetterSites: List[String],
	userSites: List[String]
)

case class SiteConfig(
	carrier: String,
	grid: Integer,
	site: Integer
)

case class TransporterBlacklistConfig(
	roma_? : Option[Integer],
	vector_? : Option[String],
	site_? : Option[String]
)

case class SealerProgramConfig(
	//@ConfigProperty var name: String = null
	//@ConfigProperty var device: String = null
	model: String,
	filename: String
)
