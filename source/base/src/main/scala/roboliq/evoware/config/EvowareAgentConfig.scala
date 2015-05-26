package roboliq.evoware.config

import roboliq.input.ProtocolDetails

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
	name: String,
	evowareDir: String,
	protocolDetails_? : Option[ProtocolDetails],
	evowareProtocolData_? : Option[EvowareProtocolData],
	tableSetups: Map[String, EvowareTableSetupConfig]
)

case class EvowareProtocolData(
	sites: Map[String, EvowareSiteConfig],
	devices: Map[String, EvowareDeviceConfig],
	transporterBlacklist: List[EvowareTransporterBlacklistConfig], // REFACTOR: I don't think this is actually used -- the processing happens directly in ConfigEvoware using TransporterBlacklistBean
	userSites: List[String]
) {
	def merge(that: EvowareProtocolData): EvowareProtocolData = {
		EvowareProtocolData(
			sites = sites ++ that.sites,
			devices = devices ++ that.devices,
			transporterBlacklist = (transporterBlacklist ++ that.transporterBlacklist).distinct,
			userSites = (userSites ++ that.userSites).distinct
		)
	}
}

object EvowareProtocolData {
	val empty = EvowareProtocolData(Map(), Map(), Nil, Nil)
}

case class EvowareSiteConfig(
	carrier_? : Option[String] = None,
	grid_? : Option[Int] = None,
	site_? : Option[Int] = None
)

case class EvowareDeviceConfig(
	`type`: String,
	evowareName: String,
	sitesOverride: List[String]
)

case class EvowareTransporterBlacklistConfig(
	roma_? : Option[Integer],
	vector_? : Option[String],
	site_? : Option[String]
)

/**
 * @param tableFile Path to evoware table file
 * @param sites Site definitions
 * @param pipetterSites List of sites the pipetter can access
 * @param userSites List of sites the user can directly access
 */
case class EvowareTableSetupConfig(
	tableFile: String,
	protocolDetails_? : Option[ProtocolDetails],
	evowareProtocolData_? : Option[EvowareProtocolData]
)