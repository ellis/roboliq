package roboliq.evoware.config

import scala.collection.JavaConversions._
import roboliq.evoware.translator.EvowareSealerProgram
import scala.collection.mutable.ArrayBuffer
import scalax.collection.edge.LkUnDiEdge
import roboliq.evoware.handler.EvowareInfiniteM200InstructionHandler
import roboliq.evoware.translator.EvowareConfig
import roboliq.evoware.translator.EvowareConfigData
import roboliq.evoware.translator.EvowareClientScriptBuilder
import roboliq.core.RsResult
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.MultiMap
import roboliq.core.RsSuccess
import roboliq.core.RqResult
import roboliq.entities.EntityBase
import grizzled.slf4j.Logger
import roboliq.entities.SiteModel
import roboliq.entities.Transporter
import roboliq.entities.ClientScriptBuilder
import roboliq.entities.TransporterSpec
import roboliq.entities.ShakerSpec
import roboliq.entities.Peeler
import roboliq.entities.ThermocyclerSpec
import roboliq.entities.SealerSpec
import roboliq.entities.Thermocycler
import roboliq.entities.Device
import roboliq.entities.Shaker
import roboliq.entities.Sealer
import roboliq.entities.LabwareModel
import roboliq.entities.PlateModel
import roboliq.entities.Rel
import roboliq.entities.Site
import roboliq.entities.Pipetter
import roboliq.entities.PeelerSpec
import roboliq.entities.LiquidVolume
import roboliq.entities.Tip
import roboliq.entities.TipModel
import roboliq.entities.Agent
import roboliq.entities.Reader
import roboliq.evoware.parser.EvowareLabwareModel
import roboliq.entities.Device
import roboliq.evoware.handler.EvowareDeviceInstructionHandler
import roboliq.evoware.parser.CarrierSite
import roboliq.evoware.parser.Carrier
import roboliq.entities.Entity
import roboliq.core.RsError
import roboliq.entities.Centrifuge
import roboliq.evoware.handler.EvowareHettichCentrifugeInstructionHandler
import roboliq.entities.SiteModel
import roboliq.evoware.handler.EvowareTRobotInstructionHandler
import roboliq.input.RjsMap
import roboliq.input.RjsNumber
import roboliq.ai.strips
import roboliq.input.RjsText
import roboliq.core.ResultC
import roboliq.input.ProtocolDataA
import roboliq.input.RjsString
import roboliq.input.RjsTypedMap
import roboliq.input.ProtocolDataABuilder





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




/**
 * @param postProcess_? An option function to call after all sites have been created, which can be used for further handling of device configuration.
 */
private case class DeviceConfigPre(
	val typeName: String,
	val deviceName_? : Option[String] = None,
	val device_? : Option[RjsTypedMap] = None,
	val overrideCreateSites_? : Option[(Carrier, Map[(Int, Int), Set[LabwareModel]]) => List[DeviceSiteConfig]] = None,
	val overrideSiteLogic_? : Option[DeviceConfig => RsResult[List[Rel]]] = None
)

private case class DeviceSiteConfig(
	siteE: CarrierSite,
	site: Site,
	labwareModel_l: List[LabwareModel]
)

private case class DeviceConfig(
	carrierE: Carrier,
	typeName: String,
	deviceName: String,
	device: Device,
	handler_? : Option[EvowareDeviceInstructionHandler],
	siteConfig_l: List[DeviceSiteConfig],
	overrideSiteLogic_? : Option[DeviceConfig => RsResult[List[Rel]]]
)

/**
 * Load evoware configuration
 */
class ConfigEvoware(
	//eb: EntityBase,
	agentName: String,
	carrierData: roboliq.evoware.parser.EvowareCarrierData,
	tableData: roboliq.evoware.parser.EvowareTableData,
	agentConfig: EvowareAgentConfig,
	tableSetupConfig: TableSetupConfig,
	offsiteModel: SiteModel,
	userArm: Transporter,
	//userArmSpec = TransporterSpec("userArmSpec")
	specToString_l: List[(String, String)],
	deviceToSpec_l: List[(String, String)],
	deviceToModelToSpec_l: List[(String, String, String)]
) {
	private val logger = Logger[this.type]

	// FIXME: can we build this up immutably instead and return it from loadEvoware()?
	val identToAgentObject = new HashMap[String, Object]

	/**
	 * Challenges when reading in Evoware configuration files:
	 * 
	 * There are lots of labware models we don't want to use, so we need to filter them out somehow.
	 * A carrier may have mutually exclusive sites, so in such cases, we need to filter out the ones that can't be used on the current table.
	 * When a table has tube labware on which cannot be moved by the RoMa, that labware should be treated as a site for tubes.
	 * What to do with other labware on the table definition?  One thing we should probably do is add it to this list of labware models we're interested in.
	 * We want to use site models, but these are not declared in Evoware, so we'll need to extract them indirectly. 
	 */
	def loadEvoware(
	): ResultC[ProtocolDataA] = {
		val identToAgentObject = new HashMap[String, Object]

		// FIXME: this should not be hard-coded -- some robots have no pipetters, some have more than one...
		val pipetterName = agentName+"__pipetter1"

		val labwareModel_m: Map[String, LabwareModelConfig]
			= if (agentConfig.labwareModels == null) Map() else agentConfig.labwareModels.toList.toMap

		for {
			data1 <- loadAgentPlateModels(labwareModel_m)
			
			siteEToSite_l <- loadSitesAndDevices(agentName, pipetterName, data1)
			_ <- loadTransporters(agentName, siteEToSite_l)
			sealerProgram_l <- loadSealerPrograms()
		} yield {
			val data0 = new ProtocolDataA(planningDomainObjects = Map(
				agentName -> "agent",
				// FIXME: this should not be hard-coded -- some robots have no pipetters, some have more than one...
				pipetterName -> "pipetter"
			))
			data0
		}
	}

	private def plateModelEToRjs(
		label_? : Option[String],
		mE: EvowareLabwareModel
	): RjsTypedMap = {
		val rjsPlateModel = RjsTypedMap("PlateModel", label_?.map(s => "label" -> RjsString(s)).toList.toMap ++ Map(
			"evowareName" -> RjsString(mE.sName),
			"rowCount" -> RjsNumber(mE.nRows),
			"colCount" -> RjsNumber(mE.nCols),
			"maxVolume" -> RjsNumber(mE.ul, Some("ul"))
		))
		rjsPlateModel
	}
	
	// Load PlateModels
	private def loadAgentPlateModels(
		labwareModel_m: Map[String, LabwareModelConfig]
	): ResultC[ProtocolDataA] = {
		val builder = new ProtocolDataABuilder
		
		// Get the list of evoware labwares that we're interested in
		// Add labware on the table definition to the list of labware we're interested in
		val labwareNamesOfInterest_l: Set[String] = (labwareModel_m.values.map(_.evowareName) ++ tableData.mapSiteToLabwareModel.values.map(_.sName)).toSet
		logger.debug("labwareNamesOfInterest_l: "+labwareNamesOfInterest_l)
		
		// Start with gathering list of all available labware models whose names are either in the config or table template
		val labwareModelE0_l = carrierData.models.collect({case m: roboliq.evoware.parser.EvowareLabwareModel if labwareNamesOfInterest_l.contains(m.sName) => m})
		//labwareModelE0_l.foreach(m => println(m.sName))
		val evowareNameToLabwareModel_m = agentConfig.labwareModels.map(kv => kv._2.evowareName -> kv).toMap
		// Only keep the ones we have LabwareModels for (TODO: seems like a silly step -- should probably filter labwareNamesOfInterest_l to begin with)
		val labwareModelE_l = labwareModelE0_l.filter(mE => evowareNameToLabwareModel_m.contains(mE.sName))
		for {
			_ <- ResultC.foreach(labwareModelE_l) { mE => 
				val modelKV = evowareNameToLabwareModel_m(mE.sName)
				val modelName = modelKV._1
				// FIXME: for debug only
				if (mE.sName == "Systemliquid") {
					println("mE: "+mE)
				}
				// ENDFIX
				//println("mE.sName: "+mE.sName)
				//val m = PlateModel(model.name, Option(model.label), Some(mE.sName), mE.nRows, mE.nCols, LiquidVolume.ul(mE.ul))
				val rjsPlateModel = plateModelEToRjs(modelKV._2.label_?, mE)
				builder.addObject(modelName, rjsPlateModel)
				// All models can be offsite
				builder.appendStackable("offsite", modelName)
				// The user arm can handle all models
				builder.appendDeviceModel("userArm", modelName)
				ResultC.unit(())
			}
		} yield builder.get
	}
	
	/**
	 * @returns Map from SiteID to its Site and the LabwareModels it can accept
	 */
	private def loadSitesAndDevices(
		agentName: String,
		pipetterName: String,
		data0: ProtocolDataA
		//modelEToModel_l: List[(EvowareLabwareModel, LabwareModel)]
		//labwareModelE_l: List[roboliq.evoware.parser.EvowareLabwareModel],
		//idToModel_m: HashMap[String, LabwareModel]
	): ResultC[ProtocolDataA] = {
		val builder = new ProtocolDataABuilder
		val carriersSeen_l = new HashSet[Int]

		def createDeviceName(carrierE: Carrier): String = {
			//println("createDeviceName: "+agentName + "__" + carrierE.sName.map(c => if (c.isLetterOrDigit) c else '_'))
			agentName + "__" + carrierE.sName.map(c => if (c.isLetterOrDigit) c else '_')
		}
		
		val modelNameToEvowareName_l: List[(String, String)] = data0.objects.map.toList.collect({ case (name, tm: RjsTypedMap) if tm.type == "PlateModel" && tm.map.contains("evowareName") => name -> tm.map("evowareName") })
		val evowareNameToModelName_l = modelNameToEvowareName_l.map(_.swap)
		// Start with gathering list of all available labware models whose names are either in the config or table template
		val labwareModelE0_l = carrierData.models.collect({case m: roboliq.evoware.parser.EvowareLabwareModel if labwareNamesOfInterest_l.contains(m.sName) => m})
		//labwareModelE0_l.foreach(m => println(m.sName))
		val evowareNameToLabwareModel_m = agentConfig.labwareModels.map(kv => kv._2.evowareName -> kv).toMap
		// Only keep the ones we have LabwareModels for (TODO: seems like a silly step -- should probably filter labwareNamesOfInterest_l to begin with)
		val labwareModelE_l = labwareModelE0_l.filter(mE => evowareNameToLabwareModel_m.contains(mE.sName))

		// Create map from siteId to all labware models that can be placed that site 
		val siteIdToModels_m: Map[(Int, Int), Set[LabwareModel]] = {
			val l = for {
				(mE, m) <- modelEToModel_l
				siteId <- mE.sites
			} yield { siteId -> m }
			l.groupBy(_._1).mapValues(_.map(_._2).toSet)
		}
		
		// Create Hotel Sites
		val siteHotel_l = for {
			o <- tableData.lHotelObject
			carrierE = o.parent
			_ = carriersSeen_l += carrierE.id
			site_i <- List.range(0, carrierE.nSites)
			site <- createSite(carrierE, site_i, s"${agentName} hotel ${carrierE.sName} site ${site_i+1}").toList
		} yield site
		
		// System liquid
		val siteSystem_? = for {
			o <- tableData.lExternalObject.find(_.carrier.sName == "System")
			site <- createSite(o.carrier, -1, s"${agentName} System Liquid")
		} yield site
			
		// Create Device and their Sites
		val deviceConfig_l = for {
			o <- tableData.lExternalObject if !carriersSeen_l.contains(o.carrier.id)
			carrierE = o.carrier
			_ = carriersSeen_l += carrierE.id
			dcp <- loadDevice(carrierE).toList
		} yield {
			val deviceName: String = dcp.deviceName_?.getOrElse(createDeviceName(carrierE))
			val device = dcp.device_?.getOrElse(new Device { val key = gid; val label = Some(carrierE.sName); val description = None; val typeNames = List(dcp.typeName) })
			val fnCreateSites: (Carrier, Map[(Int, Int), Set[LabwareModel]]) => List[DeviceSiteConfig]
				= dcp.overrideCreateSites_?.getOrElse(getDefaultDeviceSiteConfigs _)
			val siteConfig_l = fnCreateSites(carrierE, siteIdToModels_m)
			DeviceConfig(
				carrierE,
				dcp.typeName,
				deviceName,
				device,
				dcp.handler_?,
				siteConfig_l,
				dcp.overrideSiteLogic_?
			)
		}
		
		// Create on-bench Sites for Plates
		val siteBench_l = for {
			(carrierE, grid_i) <- tableData.mapCarrierToGrid.toList if !carriersSeen_l.contains(carrierE.id)
			site_i <- List.range(0, carrierE.nSites)
			site <- createSite(carrierE, site_i, s"${agentName} bench ${carrierE.sName} site ${site_i+1}").toList
		} yield {
			carriersSeen_l += carrierE.id
			site
		}

		// Concatenate hotel, device, and bench sites
		val site_l = {
			val siteDevice_l = deviceConfig_l.flatMap(_.siteConfig_l.map(sc => (sc.siteE, sc.site)))
			siteSystem_?.toList ++ siteHotel_l ++ siteDevice_l ++ siteBench_l
		}
		
		// TODO: For tubes, create their on-bench Sites and SiteModels
		// TODO: Let userArm handle tube models
		// TODO: Let userArm access all sites that the robot arms can't
		
		// Update EntityBase with sites and logic
		// Update identToAgentObject map with evoware site data
		for ((siteE, site) <- site_l) {
			val siteName = site.label.get
			val siteId = (siteE.carrier.id, siteE.iSite)
			identToAgentObject(siteName) = siteE
			builder.addSite(siteName)

			// Make site pipetter-accessible if it's listed in the table setup's `pipetterSites`
			if (tableSetupConfig.pipetterSites.contains(siteName)) {
				builder.addDeviceSite(pipetterName, siteName)
			}
			// Make site user-accessible if it's listed in the table setup's `userSites`
			if (tableSetupConfig.userSites.contains(siteName)) {
				builder.addTransporterCan("userArm", siteName, "userArmSpec")
			}
		}
		
		// Create system liquid labware
		siteSystem_?.foreach { carrierToSite =>
			val site = carrierToSite._2
			val siteModelName = "sm0"
			val plateModelName = "SystemLiquid"
			val mE = EvowareLabwareModel(plateModelName, 8, 1, 1000, List((-1, -1)))
			val rjsPlateModel = plateModelEToRjs(None, mE)
			val sm = SiteModel("SystemLiquid")
			builder.addPlateModel(plateModelName, rjsPlateModel)
			builder.addSiteModel(siteModelName)
			builder.appendStackable(siteModelName, plateModelName)
			builder.setModel(site, siteModelName)
		}		
		
		// Create SiteModels for for sites which hold Plates
		{
			// Find all unique sets of labware models
			val unique = siteIdToModels_m.values.toSet
			// Create SiteModel for each unique set of labware models
			val modelsToSiteModel_m: Map[Set[LabwareModel], SiteModel] =
				unique.toList.map(l => l -> SiteModel(l.toString)).toMap

			// Update EntitBase with the SiteModels and their stackable LabwareModels
			for (((l, sm), i) <- modelsToSiteModel_m.toList.zipWithIndex) {
				val siteModelName = s"sm${i+1}"
				builder.addSiteModel(siteModelName)
				builder.appendStackables(siteModelName, l.toList)
			}

			// Update EntityBase by assigning SiteModel to a site Sites
			for ((siteE, site) <- site_l) {
				val siteId = (siteE.carrier.id, siteE.iSite)
				for {
					l <- siteIdToModels_m.get(siteId)
					sm <- modelsToSiteModel_m.get(l)
				} {
					builder.setModel(site, sm)
				}
			}
		}

		// Update EntityBase with devices and some additional logic regarding their sites
		for (dc <- deviceConfig_l) {
			builder.addDevice(agentName, dc.device, dc.deviceName)
			dc.handler_? match {
				case Some(handler) => 
					// Bind deviceName to the handler instead of to the carrier
					identToAgentObject(dc.deviceName) = handler
				case _ =>
					// Bind deviceName to the carrier
					identToAgentObject(dc.deviceName) = dc.carrierE
			}
			
			// Add device sites
			dc.overrideSiteLogic_?.getOrElse(getDefaultSitesLogic _)(dc) match {
				case RsSuccess(rel_l, _) =>
					rel_l.foreach(eb.addRel)
				case RsError(e, w) => return RsError(e, w)
			}
		}
		
		RsSuccess(site_l)
	}

	private def createSite(carrierE: roboliq.evoware.parser.Carrier, site_i: Int, description: String): Option[(CarrierSite, Site)] = {
		val grid_i = tableData.mapCarrierToGrid(carrierE)
		// TODO: should adapt CarrierSite to require grid_i as a parameter 
		findSiteName(tableSetupConfig, carrierE.sName, grid_i, site_i + 1).map { siteName =>
			val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
			val site = Site(gid, Some(siteName), Some(description))
			(siteE, site)
		}
	}

	private def findSiteName(tableSetupConfig: TableSetupConfig, carrierName: String, grid: Int, site: Int): Option[String] = {
		def ok[A](a: A, b: A): Boolean = (a == null || a == b)
		def okInt(a: Integer, b: Int): Boolean = (a == null || a == b)
		def matches(siteConfig: SiteConfig): Boolean =
			ok(siteConfig.carrier, carrierName) &&
			okInt(siteConfig.grid, grid) &&
			okInt(siteConfig.site, site)
		for ((ident, bean) <- tableSetupConfig.sites.toMap) {
			if (matches(bean))
				return Some(ident)
		}
		None
	}
	
	private def getDefaultDeviceSiteConfigs(
		carrierE: Carrier,
		siteIdToModels_m: Map[(Int, Int), Set[LabwareModel]]
	): List[DeviceSiteConfig] = {
		for {
			site_i <- List.range(0, carrierE.nSites)
			(siteE, site) <- createSite(carrierE, site_i, s"${agentName} device ${carrierE.sName} site ${site_i+1}").toList
			siteId = (carrierE.id, site_i)
			model_l <- siteIdToModels_m.get(siteId).toList
		} yield {
			DeviceSiteConfig(siteE, site, model_l.toList)
		}
	}
	
	private def getDefaultSitesLogic(dc: DeviceConfig): RsResult[List[Rel]] = {
		for {
			ll <- RsResult.mapAll(dc.siteConfig_l){ sc => getDefaultSiteLogic(dc, sc) }
		} yield ll.flatten
	}
	
	private def getDefaultSiteLogic(dc: DeviceConfig, sc: DeviceSiteConfig): RsResult[List[Rel]] = {
		val siteName = sc.site.label.get
		val logic1 = Rel("device-can-site", List(dc.deviceName, sc.site.label.get))
		val logic2_l = sc.labwareModel_l.map(m => Rel("device-can-model", List(dc.deviceName, eb.getName(m).toOption.get)))
		RsSuccess(logic1 :: logic2_l)
	}
	
	private def loadTransporters(
		agentName: String,
		siteEToSite_l: List[(CarrierSite, Site)]
	): RqResult[Unit] = {

		// Most CarrierSites are associated with a single Site
		// However, a carousel will have multiple internal sites associated with a single CarrierSite
		val siteEToSites_m: Map[CarrierSite, List[Site]] = {
			siteEToSite_l
				.groupBy(_._1) // Group by CarrierSite
				.mapValues(_.map(_._2)) // Extract the list of sites for each CarrierSite
				.toMap
		}
		
		val objectToType_m = new HashMap[String, String]
		val agentData_m = new HashMap[String, RjsMap]
		val literal_l = new ArrayBuffer[strips.Literal]
		
		val romaIdToName_m = new HashMap[Int, String]
		val vectorIdToName_m = new HashMap[String, String]
		
		// Create transporters
		{
			// List of RoMa indexes
			val roma_li = carrierData.mapCarrierToVectors.values.flatMap(_.map(_.iRoma)).toSet
			// Add transporter device for each RoMa
			for (roma_i <- roma_li) {
				val ident = s"${agentName}__transporter${roma_i + 1}"
				romaIdToName_m(roma_i) = ident
				objectToType_m(ident) = "transporter"
				agentData_m(ident) = RjsMap("roma" -> RjsNumber(roma_i))
			}
		}
		
		// Create transporter specs
		
		{
			val vectorClass_l: List[String] = carrierData.mapCarrierToVectors.toList.flatMap(_._2).map(_.sClass).toSet.toList.sorted
			var vector_i = 0
			for (vectorClass <- vectorClass_l) {
				val ident = s"${agentName}__transporterSpec${vector_i}"
				vectorIdToName_m(vectorClass) = ident
				objectToType_m(ident) = "transporterSpec"
				agentData_m(ident) = RjsMap("vectorClass" -> RjsText(vectorClass))
				//println(ident)
				for (romaName <- romaIdToName_m.values) {
					literal_l += strips.Literal(true, "device-can-spec", romaName, ident)
				}
				if (!romaIdToName_m.isEmpty)
					vector_i += 1
			}
		}
		
		// Find which sites the transporters can access
		val agentRomaVectorToSite_m: Map[(String, String, String), List[Site]] = {
			val transporterBlacklist_l = {
				if (agentConfig.transporterBlacklist == null)
					Nil
				else
					agentConfig.transporterBlacklist.toList
			}
			def isBlacklisted(roma: Int, vector: String, siteName: String): Boolean = {
				transporterBlacklist_l.exists { item =>
					val romaMatches = item.roma == null || item.roma == roma
					val vectorMatches = item.vector == null || item.vector == vector
					val siteMatches = item.site == null || item.site == siteName
					romaMatches && vectorMatches && siteMatches
				}
			}
			
			val agentRomaVectorToSite_m = new HashMap[(String, String, String), List[Site]]
			for {
				(carrierE, vector_l) <- carrierData.mapCarrierToVectors.toList
				site_i <- List.range(0, carrierE.nSites)
				siteE = CarrierSite(carrierE, site_i)
				site <- siteEToSites_m.getOrElse(siteE, Nil)
				vector <- vector_l
			} {
				val deviceName = romaIdToName_m(vector.iRoma)
				val siteName = eb.entityToName_m(site)
				val specName = vectorIdToName_m(vector.sClass)
				val key = (agentName, deviceName, vector.sClass)
				if (isBlacklisted(vector.iRoma + 1, vector.sClass, siteName)) {
					logger.info(s"blacklisted vector $key for site $siteName")
				}
				else {
					agentRomaVectorToSite_m(key) = site :: agentRomaVectorToSite_m.getOrElse(key, Nil)
					literal_l += strips.Literal(true, "transporter-can", deviceName, siteName, specName)
				}
			}
			agentRomaVectorToSite_m.toMap
		}

		val graph = {
			// Populate graph from entries in agentRomaVectorToSite_m
			import scalax.collection.Graph // or scalax.collection.mutable.Graph
			import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._
			import scalax.collection.edge.LHyperEdge
			val edge_l = agentRomaVectorToSite_m.toList.flatMap(pair => {
				val (key, site_l) = pair
				site_l.combinations(2).map(l => LkUnDiEdge(l(0), l(1))(key))
			})
			//edge_l.take(5).foreach(println)
			Graph[Site, LkUnDiEdge](edge_l : _*)
		}

		val userGraph = {
			// Add user-accessible sites into the graph
			val offsite = eb.getEntityAs[Site]("offsite").getOrElse(null)
			val agentRomaVectorToSite_m: Map[(String, String, String), List[Site]] = Map( 
				("user", "", "") -> (offsite :: RqResult.flatten(tableSetupConfig.userSites.toList.map(eb.getEntityAs[Site])))
			)
			// Populate graph from entries in agentRomaVectorToSite_m
			import scalax.collection.Graph // or scalax.collection.mutable.Graph
			import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._
			import scalax.collection.edge.LHyperEdge
			val edge_l = agentRomaVectorToSite_m.toList.flatMap(pair => {
				val (key, site_l) = pair
				site_l.combinations(2).map(l => LkUnDiEdge(l(0), l(1))(key))
			})
			//edge_l.take(5).foreach(println)
			Graph[Site, LkUnDiEdge](edge_l : _*)
		}

		// FIXME: should append to transportGraph (not replace it) so that we can have multiple evoware agents
		eb.transportGraph = graph
		eb.transportUserGraph = userGraph
		//println("graph: "+graph.size)
		//graph.take(5).foreach(println)
		//graph.foreach(println)
		
		RsSuccess(())
	}
	
	/**
	 * TODO: most of this should be loaded from a configuration file
	 */
	private def loadDevice(carrierE: Carrier): Option[DeviceConfigPre] = {
		/*typeName: String,
		deviceName_? : Option[String],
		device_? : Option[Device],
		handler_? : Option[EvowareDeviceInstructionHandler],
		site_l_? : Option[List[Site]]*/
		carrierE.partNo_?.getOrElse(carrierE.sName) match {
			// System liquid
			case "System" =>
				None
				
			// Infinite M200
			case "Tecan part no. 30016056 or 30029757" =>
				// In addition to the default logic, also let the device open its site
				def overrideSiteLogic(dc: DeviceConfig): RsResult[List[Rel]] = {
					for {
						default_l <- getDefaultSitesLogic(dc)
					} yield {
						val l = dc.siteConfig_l.map { sc =>
							val siteName = sc.site.label.get
							Rel("device-can-open-site", List(dc.deviceName, siteName))
						}
						default_l ++ l 
					}
				}
				Some(new DeviceConfigPre(
					"reader",
					device_? = Some(new Reader(gid, Some(carrierE.sName))),
					overrideSiteLogic_? = Some(overrideSiteLogic)
				))

			case "MP 2Pos H+P Shake" =>
				// HACK: only use last site for shaking, this is truly a bad hack!  Things like this should be performed via configuration overrides.
				def overrideSiteLogic(dc: DeviceConfig): RsResult[List[Rel]] = {
					for {
						// For the first site, there's no device logic, since the device can't use that site
						// Get default logic for the second site
						logic2_l <- getDefaultSiteLogic(dc, dc.siteConfig_l(1))
					} yield logic2_l
				}
				Some(DeviceConfigPre(
					"shaker",
					device_? = Some(new Shaker(gid, Some(carrierE.sName))),
					overrideSiteLogic_? = Some(overrideSiteLogic)
				))

			case "RoboPeel" =>
				Some(new DeviceConfigPre(
					"peeler",
					device_? = Some(new Peeler(gid, Some(carrierE.sName)))
				))
				
				
			case "RoboSeal" =>
				Some(new DeviceConfigPre(
					"sealer",
					device_? = Some(new Sealer(gid, Some(carrierE.sName)))
				))
				
			// Te-Shake 2Pos
			case "Tecan part no. 10760722 with 10760725" =>
				Some(new DeviceConfigPre(
					"shaker",
					device_? = Some(new Shaker(gid, Some(carrierE.sName)))
				))
				
			case "TRobot1" =>
				// In addition to the default logic, also let the device open its site
				def overrideSiteLogic(dc: DeviceConfig): RsResult[List[Rel]] = {
					for {
						default_l <- getDefaultSitesLogic(dc)
					} yield {
						val l = dc.siteConfig_l.map { sc =>
							val siteName = sc.site.label.get
							Rel("device-can-open-site", List(dc.deviceName, siteName))
						}
						default_l ++ l 
					}
				}
				Some(new DeviceConfigPre(
					"thermocycler",
					device_? = Some(new Thermocycler(gid, Some(carrierE.sName))),
					handler_? = Some(new EvowareTRobotInstructionHandler(carrierE)),
					overrideSiteLogic_? = Some(overrideSiteLogic)
				))
			
			case "Centrifuge" => // TODO: FIXME: Get the correct ID or name
				// Add the four internal sites, each pointing to the centrifuge's single external site
				def overrideCreateSites(
					carrierE: Carrier,
					siteIdToModels_m: Map[(Int, Int), Set[LabwareModel]]
				): List[DeviceSiteConfig] = {
					val siteExternal_i = 0
					val siteE = CarrierSite(carrierE, siteExternal_i)
					val siteId = (carrierE.id, siteExternal_i)
					val siteExternal_? = createSite(carrierE, siteExternal_i, "")
					val siteExternalName_? = siteExternal_?.map(_._2.label.get)
					
					for {
						siteNameBase <- siteExternalName_?.toList
						siteInternal_i <- List.range(0, 4)
						site = Site(gid, Some(s"${siteNameBase}_${siteInternal_i+1}"), Some(s"internal site ${siteInternal_i+1}"))
						model_l <- siteIdToModels_m.get(siteId).toList
					} yield {
						//println("site: "+site)
						DeviceSiteConfig(siteE, site, model_l.toList)
					}
				}

				// let the device open its sites
				// sites are closed by default
				def overrideSiteLogic(dc: DeviceConfig): RsResult[List[Rel]] = {
					for {
						default_l <- getDefaultSitesLogic(dc)
					} yield {
						val l = dc.siteConfig_l.map { sc =>
							val siteName = sc.site.label.get
							Rel("site-closed", List(siteName))
						}
						default_l ++ l 
					}
				}

				Some(new DeviceConfigPre(
					"Centrifuge",
					overrideCreateSites_? = Some(overrideCreateSites),
					overrideSiteLogic_? = Some(overrideSiteLogic)
				))
				// TODO: Add operators for opening the sites and for closing the device
				
			case _ =>
				None
		}
	}
	
	def buildSealerPrograms(builder: ProtocolDataABuilder): ResultC[Unit] = {
		for ((name, sealerConfig) <- agentConfig.sealerPrograms) {
			val rjsSealerProgram = RjsTypedMap("SealerProgram", Map(
				"model" -> RjsString(sealerConfig.model),
				"filename" -> RjsString(sealerConfig.filename)
			))
			builder.addObject(name, rjsSealerProgram)
			builder.addPlanningDomainObject(name, "SealerProgram")
		}
		ResultC.unit(())
	}
	
	/*
	private def loadDevices(
		agent: Agent,
		siteIdToSiteAndModels_m: Map[(Int, Int), (Site, Set[LabwareModel])]
		//siteIdToModels_m: HashMap[(Int, Int), collection.mutable.Set[LabwareModel]] with MultiMap[(Int, Int), LabwareModel],
		//idToModel_m: Map[String, LabwareModel]
	): RsResult[Unit] = {
		
		def getDeviceSitesAndModels(
			carrierE: roboliq.evoware.parser.Carrier
		): List[(Site, Set[LabwareModel])] = {
			//val l = new ArrayBuffer[(Site, List[LabwareModel])]
			for {
				site_i <- (0 until carrierE.nSites).toList
				siteId = (carrierE.id, site_i)
				siteToModels <- siteIdToSiteAndModels_m.get(siteId).toList
			} yield siteToModels
		}
		
		def addDeviceOnly(
			device: Device,
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		) {
			// Add device
			eb.addDevice(agent, device, deviceName)
			identToAgentObject(deviceName) = carrierE
		}
		
		def addDeviceAndSitesAndModels(
			device: Device,
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		) {
			addDeviceOnly(device, deviceName, carrierE)
	
			// Add device sites
			getDeviceSitesAndModels(carrierE).foreach { case (site, model_l) =>
				eb.addDeviceSite(device, site)
				model_l.foreach(m => eb.addDeviceModel(device, m))
			}
		}
		
		def addDevice(
			typeName: String,
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			// Add device
			val device = new Device { val key = gid; val label = Some(carrierE.sName); val description = None; val typeNames = List(typeName) }
			addDeviceAndSitesAndModels(device, deviceName, carrierE)
			device
		}
		
		def addPeeler(
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			val device = new Peeler(gid, Some(carrierE.sName))
			addDeviceAndSitesAndModels(device, deviceName, carrierE)
			device
		}
		
		def addSealer(
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			val device = new Sealer(gid, Some(carrierE.sName))
			addDeviceAndSitesAndModels(device, deviceName, carrierE)
			device
		}
		
		def addShaker(
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			val device = new Shaker(gid, Some(carrierE.sName))
			addDeviceAndSitesAndModels(device, deviceName, carrierE)
			device
		}
		
		def createDeviceName(carrierE: roboliq.evoware.parser.Carrier): String = {
			agentName + "__" + carrierE.sName.map(c => if (c.isLetterOrDigit) c else '_')
		}
		
		//println("Carriers: " + tableData.mapCarrierToGrid.keys.mkString("\n"))
		for ((carrierE, iGrid) <- tableData.mapCarrierToGrid) {
			//println(s"carrier: $iGrid, ${carrierE.sName}, ${carrierE.partNo_?}")
			carrierE.partNo_?.getOrElse(carrierE.sName) match {
				// Infinite M200
				case "Tecan part no. 30016056 or 30029757" =>
					val deviceName = createDeviceName(carrierE)
					val handler = new EvowareInfiniteM200InstructionHandler(carrierE)
					val device = new Reader(gid, Some(carrierE.sName))
					
					eb.addDevice(agent, device, deviceName)
					// Bind deviceName to the handler instead of to the carrier
					identToAgentObject(deviceName) = handler
					// Add device sites
					getDeviceSitesAndModels(carrierE).foreach { case (site, model_l) =>
						val siteName = eb.getName(site).toOption.get
						eb.addDeviceSite(device, site)
						model_l.foreach(m => eb.addDeviceModel(device, m))
						eb.addRel(Rel("device-can-open-site", List(deviceName, siteName)))
					}

				case "MP 2Pos H+P Shake" =>
					val deviceName = createDeviceName(carrierE)
					// REFACTOR: duplicates addShaker(), because for this device, only the second site can actually be used for shaking
					val device = new Shaker(gid, Some(carrierE.sName))
					// Add device
					eb.addDevice(agent, device, deviceName)
					identToAgentObject(deviceName) = carrierE
					// Add device sites
					// HACK: only use last site for shaking, this is truly a bad hack!  Things like this should be performed via configuration overrides.
					for (site_i <- List(carrierE.nSites - 1)) {
						val siteId = (carrierE.id, site_i)
						siteIdToSiteAndModels_m.get(siteId) match {
							case Some((site, model_l)) =>
								eb.addDeviceSite(device, site)
								model_l.foreach(m => eb.addDeviceModel(device, m))
							case None =>
						}
					}

				case "RoboPeel" =>
					val deviceName = createDeviceName(carrierE)
					val device = addPeeler(deviceName, carrierE)
					// Add user-defined specs for this device
					for ((deviceName2, plateModelName, specName) <- deviceToModelToSpec_l if deviceName2 == deviceName) {
						// Get or create the sealer spec for specName
						val spec: PeelerSpec = eb.getEntity(specName) match {
							case Some(spec) => spec.asInstanceOf[PeelerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specName).get._2
								identToAgentObject(specName) = internal
								PeelerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specName)
						// Let entity base know that that the spec can be used for the plate model
						eb.addRel(Rel("device-spec-can-model", List(deviceName, specName, plateModelName)))
					}
					
				case "RoboSeal" =>
					val deviceName = createDeviceName(carrierE)
					val device = addSealer(deviceName, carrierE)
					// Add user-defined specs for this device
					for ((deviceName2, plateModelName, specName) <- deviceToModelToSpec_l if deviceName2 == deviceName) {
						// Get or create the sealer spec for specName
						val spec: SealerSpec = eb.getEntity(specName) match {
							case Some(spec) => spec.asInstanceOf[SealerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specName).get._2
								identToAgentObject(specName) = internal
								SealerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specName)
						// Let entity base know that that the spec can be used for the plate model
						eb.addRel(Rel("device-spec-can-model", List(deviceName, specName, plateModelName)))
					}
					
				// Te-Shake 2Pos
				case "Tecan part no. 10760722 with 10760725" =>
					val deviceName = createDeviceName(carrierE)
					val device = new Shaker(gid, Some(carrierE.sName))
					addDeviceAndSitesAndModels(device, deviceName, carrierE)
					// Add user-defined specs for this device
					for ((deviceName2, specName) <- deviceToSpec_l if deviceName2 == deviceName) {
						// Get or create the spec for specName
						val spec: ShakerSpec = eb.getEntity(specName) match {
							case Some(spec) => spec.asInstanceOf[ShakerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specName).get._2
								identToAgentObject(specName) = internal
								ShakerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specName)
					}
					
				case "TRobot1" =>
					val deviceName = createDeviceName(carrierE)
					val device = new Thermocycler(gid, Some(carrierE.sName))
					addDeviceAndSitesAndModels(device, deviceName, carrierE)
					// Add user-defined specs for this device
					for ((deviceName2, specName) <- deviceToSpec_l if deviceName2 == deviceName) {
						// Get or create the spec for specName
						val spec: ThermocyclerSpec = eb.getEntity(specName) match {
							case Some(spec) => spec.asInstanceOf[ThermocyclerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specName).get._2
								identToAgentObject(specName) = internal
								ThermocyclerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specName)
					}
				
				case "Hettich Centrifuge" => // TODO: FIXME: Get the correct ID or name
					// TODO: Add 4 sites, aliasing them to the device site's (grid,site)
					// TODO: Remove the device site from 'eb' (probably necessary to prevent it from being added in the first place)
					// TODO: Add operators for opening the sites and for closing the device
					//...
					
					
				case _ =>
			}
		}
		
		RsSuccess(())
	}
	*/
	
	/*
	 * TODO: need to handle shaker programs
	def loadShakerPrograms(): RsResult[List[EvowareShakerProgram]] = {
				// Add user-defined specs for this device
				for ((deviceName2, specName) <- deviceToSpec_l if deviceName2 == deviceName) {
					// Get or create the spec for specName
					val spec: ShakerSpec = eb.getEntity(specName) match {
						case Some(spec) => spec.asInstanceOf[ShakerSpec]
						case None =>
							// Store the evoware string for this spec
							val internal = specToString_l.find(_._1 == specName).get._2
							identToAgentObject(specName) = internal
							ShakerSpec(gid, None, Some(internal))
					}
					// Register the spec
					eb.addDeviceSpec(device, spec, specName)
				}
	}
	*/

	/* TODO: need to handle thermocycler programs
				// Add user-defined specs for this device
				for ((deviceName2, specName) <- deviceToSpec_l if deviceName2 == deviceName) {
					// Get or create the spec for specName
					val spec: ThermocyclerSpec = eb.getEntity(specName) match {
						case Some(spec) => spec.asInstanceOf[ThermocyclerSpec]
						case None =>
							// Store the evoware string for this spec
							val internal = specToString_l.find(_._1 == specName).get._2
							identToAgentObject(specName) = internal
							ThermocyclerSpec(gid, None, Some(internal))
					}
					// Register the spec
					eb.addDeviceSpec(device, spec, specName)
				}
	*/
}