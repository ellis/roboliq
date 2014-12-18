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
import roboliq.input.EvowareAgentBean
import roboliq.input.TableSetupBean
import roboliq.input.LabwareModelBean
import roboliq.input.SiteBean

/**
 * @param postProcess_? An option function to call after all sites have been created, which can be used for further handling of device configuration.
 */
private case class DeviceConfigPre(
	val typeName: String,
	val deviceIdent_? : Option[String] = None,
	val device_? : Option[Device] = None,
	val handler_? : Option[EvowareDeviceInstructionHandler] = None,
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
	deviceIdent: String,
	device: Device,
	handler_? : Option[EvowareDeviceInstructionHandler],
	siteConfig_l: List[DeviceSiteConfig],
	overrideSiteLogic_? : Option[DeviceConfig => RsResult[List[Rel]]]
)

/**
 * Load evoware configuration
 */
class ConfigEvoware(
	eb: EntityBase,
	agentIdent: String,
	carrierData: roboliq.evoware.parser.EvowareCarrierData,
	tableData: roboliq.evoware.parser.EvowareTableData,
	agentBean: EvowareAgentBean,
	tableSetupBean: TableSetupBean,
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

	// Generate a UUID for an entity
	// TODO: This doesn't belong here
	private def gid: String = java.util.UUID.randomUUID().toString()
	
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
	): RsResult[ClientScriptBuilder] = {
		val agent = Agent(gid, Some(agentIdent))
		eb.addAgent(agent, agentIdent)
		
		val identToAgentObject = new HashMap[String, Object]

		// FIXME: this should not be hard-coded -- some robots have no pipetters, some have more than one...
		val pipetterIdent = agentIdent+"__pipetter1"
		val pipetter = new Pipetter(gid, Some(agentIdent+" LiHa"))
		eb.addDevice(agent, pipetter, pipetterIdent)

		for {
			tuple1 <- loadAgentBean()
			(tip_l, modelEToModel_l) = tuple1
			
			siteEToSite_l <- loadSitesAndDevices(agent, pipetterIdent, modelEToModel_l)
			_ <- loadTransporters(agent, siteEToSite_l)
			sealerProgram_l <- loadSealerPrograms()
		} yield {
			eb.pipetterToTips_m(pipetter) = tip_l.toList
			
			val configData = EvowareConfigData(Map())
			val config = new EvowareConfig(carrierData, tableData, configData, sealerProgram_l)
			val scriptBuilder = new EvowareClientScriptBuilder(agentIdent, config)
			scriptBuilder
		}
	}

	/**
	 * @returns tuple of list of labware models, list of tips, list of pairs of EvowareLabwareModel and LabwareModelS
	 */
	private def loadAgentBean(
	): RsResult[(
		List[Tip],
		List[(EvowareLabwareModel, LabwareModel)]
	)] = {
		val labwareModel_l = if (agentBean.labwareModels == null) Nil else agentBean.labwareModels.toList

		// Tip models
		for {
			tipModel_l <- loadAgentTipModels()
			tip_l <- loadAgentTips(tipModel_l)
			modelEToModel_l <- loadAgentPlateModels(labwareModel_l)
		} yield (tip_l, modelEToModel_l)
	}

	private def loadAgentTipModels(
	): RsResult[List[TipModel]] = {
		// Tip models
		val tipModel_l = new ArrayBuffer[TipModel]
		if (agentBean.tipModels != null) {
			for ((id, tipModelBean) <- agentBean.tipModels.toMap) {
				val tipModel = TipModel(id, None, None, LiquidVolume.ul(BigDecimal(tipModelBean.max)), LiquidVolume.ul(BigDecimal(tipModelBean.min)), Map())
				tipModel_l += tipModel
				eb.addEntityWithoutIdent(tipModel)
			}
		}
		RsSuccess(tipModel_l.toList)
	}
	
	private def loadAgentTips(
		tipModel_l: List[TipModel]
	): RsResult[List[Tip]] = {
		// Tips
		val tip_l = new ArrayBuffer[Tip]
		val tipBean_l = if (agentBean.tips != null) agentBean.tips.toList else Nil
		for {
			_ <- RsResult.toResultOfList(tipBean_l.zipWithIndex.map { pair =>
				val (tipBean, index_) = pair
				val row: Int = if (tipBean.row == 0) index_ else tipBean.row
				// HACK: use the row as index instead, need to figure out a more general solution,
				//  such as specifying that a tip cannot be used -- ellis, 2014-02-06
				val index = row - 1
				val col = 0
				for {
					permanentTipModel_? <- if (tipBean.permanentModel == null) RsSuccess(None) else eb.getEntityAs[TipModel](tipBean.permanentModel).map(Option(_))
					tipModel2_l <- (permanentTipModel_?, tipBean.models) match {
						case (Some(tipModel), _) => RsSuccess(List(tipModel))
						case (_, null) => RsSuccess(tipModel_l.toList)
						case _ => RsResult.toResultOfList(tipBean.models.toList.map(eb.getEntityAs[TipModel](_)))
					}
				} yield {
					val tip = Tip("tip"+(index + 1), None, None, index, row, col, permanentTipModel_?)
					tip_l += tip
					eb.tipToTipModels_m(tip) = tipModel2_l.toList
				}
			})
		} yield tip_l.toList
	}
	
	// Load PlateModels
	private def loadAgentPlateModels(
		labwareModel_l: List[LabwareModelBean]
	): RsResult[List[(EvowareLabwareModel, LabwareModel)]] = {
		// Get the list of evoware labwares that we're interested in
		// Add labware on the table definition to the list of labware we're interested in
		val labwareNamesOfInterest_l: Set[String] = (labwareModel_l.map(_.evowareName) ++ tableData.mapSiteToLabwareModel.values.map(_.sName)).toSet
		logger.debug("labwareNamesOfInterest_l: "+labwareNamesOfInterest_l)
		
		// Start with gathering list of all available labware models whose names are either in the config or table template
		val labwareModelE0_l = carrierData.models.collect({case m: roboliq.evoware.parser.EvowareLabwareModel if labwareNamesOfInterest_l.contains(m.sName) => m})
		//labwareModelE0_l.foreach(m => println(m.sName))
		val evowareNameToLabwareModel_m = agentBean.labwareModels.toList.map(x => x.evowareName -> x).toMap
		// Only keep the ones we have LabwareModels for (TODO: seems like a silly step -- should probably filter labwareNamesOfInterest_l to begin with)
		val labwareModelE_l = labwareModelE0_l.filter(mE => evowareNameToLabwareModel_m.contains(mE.sName))
		for {
			modelEToModel_l <- RsResult.mapAll(labwareModelE_l) { mE => 
				for {
					model <- RsResult.from(evowareNameToLabwareModel_m.get(mE.sName), s"evoware labware model not found: `${mE.sName}`")
				} yield {
					// FIXME: for debug only
					if (mE.sName == "Systemliquid") {
						println(mE)
						println(mE)
					}
					// ENDFIX
					//println("mE.sName: "+mE.sName)
					val m = PlateModel(model.name, Option(model.label), Some(mE.sName), mE.nRows, mE.nCols, LiquidVolume.ul(mE.ul))
					//idToModel_m(mE.sName) = m
					eb.addModel(m, model.name)
					// All models can be offsite
					eb.addStackable(offsiteModel, m)
					// The user arm can handle all models
					eb.addDeviceModel(userArm, m)
					identToAgentObject(model.name) = mE
					(mE, m)
				}
			}
		} yield modelEToModel_l
	}
	
	/**
	 * @returns Map from SiteID to its Site and the LabwareModels it can accept
	 */
	private def loadSitesAndDevices(
		agent: Agent,
		pipetterIdent: String,
		modelEToModel_l: List[(EvowareLabwareModel, LabwareModel)]
		//labwareModelE_l: List[roboliq.evoware.parser.EvowareLabwareModel],
		//idToModel_m: HashMap[String, LabwareModel]
	): RsResult[List[(CarrierSite, Site)]] = {
		val carriersSeen_l = new HashSet[Int]

		def createDeviceIdent(carrierE: Carrier): String = {
			//println("createDeviceIdent: "+agentIdent + "__" + carrierE.sName.map(c => if (c.isLetterOrDigit) c else '_'))
			agentIdent + "__" + carrierE.sName.map(c => if (c.isLetterOrDigit) c else '_')
		}
		
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
			site <- createSite(carrierE, site_i, s"${agentIdent} hotel ${carrierE.sName} site ${site_i+1}").toList
		} yield site
		
		// System liquid
		val siteSystem_? = for {
			o <- tableData.lExternalObject.find(_.carrier.sName == "System")
			site <- createSite(o.carrier, -1, s"${agentIdent} System Liquid")
		} yield site
			
		// Create Device and their Sites
		val deviceConfig_l = for {
			o <- tableData.lExternalObject if !carriersSeen_l.contains(o.carrier.id)
			carrierE = o.carrier
			_ = carriersSeen_l += carrierE.id
			dcp <- loadDevice(carrierE).toList
		} yield {
			val deviceIdent: String = dcp.deviceIdent_?.getOrElse(createDeviceIdent(carrierE))
			val device = dcp.device_?.getOrElse(new Device { val key = gid; val label = Some(carrierE.sName); val description = None; val typeNames = List(dcp.typeName) })
			val fnCreateSites: (Carrier, Map[(Int, Int), Set[LabwareModel]]) => List[DeviceSiteConfig]
				= dcp.overrideCreateSites_?.getOrElse(getDefaultDeviceSiteConfigs _)
			val siteConfig_l = fnCreateSites(carrierE, siteIdToModels_m)
			DeviceConfig(
				carrierE,
				dcp.typeName,
				deviceIdent,
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
			site <- createSite(carrierE, site_i, s"${agentIdent} bench ${carrierE.sName} site ${site_i+1}").toList
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
			val siteIdent = site.label.get
			val siteId = (siteE.carrier.id, siteE.iSite)
			identToAgentObject(siteIdent) = siteE
			eb.addSite(site, siteIdent)

			// Make site pipetter-accessible if it's listed in the table setup's `pipetterSites`
			if (tableSetupBean.pipetterSites.contains(siteIdent)) {
				eb.addRel(Rel("device-can-site", List(pipetterIdent, siteIdent)))
			}
			// Make site user-accessible if it's listed in the table setup's `userSites`
			if (tableSetupBean.userSites.contains(siteIdent)) {
				eb.addRel(Rel("transporter-can", List("userArm", siteIdent, "userArmSpec")))
			}
		}
		
		// Create system liquid labware
		siteSystem_?.foreach { carrierToSite =>
			val site = carrierToSite._2
			val name = "SystemLiquid"
			val mE = EvowareLabwareModel(name, 8, 1, 1000, List((-1, -1)))
			val m = PlateModel(name, Option(name), Some(name), mE.nRows, mE.nCols, LiquidVolume.ul(mE.ul))
			val sm = SiteModel("SystemLiquid")
			eb.addModel(sm, s"sm0")
			eb.addModel(m, "SystemLiquid")
			eb.addStackables(sm, List(m))
			eb.setModel(site, sm)
			identToAgentObject(name) = mE
			carrierToSite
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
				eb.addModel(sm, s"sm${i+1}")
				eb.addStackables(sm, l.toList)
			}

			// Update EntityBase by assigning SiteModel to a site Sites
			for ((siteE, site) <- site_l) {
				val siteId = (siteE.carrier.id, siteE.iSite)
				for {
					l <- siteIdToModels_m.get(siteId)
					sm <- modelsToSiteModel_m.get(l)
				} {
					eb.setModel(site, sm)
				}
			}
		}

		// Update EntityBase with devices and some additional logic regarding their sites
		for (dc <- deviceConfig_l) {
			eb.addDevice(agent, dc.device, dc.deviceIdent)
			dc.handler_? match {
				case Some(handler) => 
					// Bind deviceIdent to the handler instead of to the carrier
					identToAgentObject(dc.deviceIdent) = handler
				case _ =>
					// Bind deviceIdent to the carrier
					identToAgentObject(dc.deviceIdent) = dc.carrierE
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
		findSiteIdent(tableSetupBean, carrierE.sName, grid_i, site_i + 1).map { siteIdent =>
			val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
			val site = Site(gid, Some(siteIdent), Some(description))
			(siteE, site)
		}
	}

	private def findSiteIdent(tableSetupBean: TableSetupBean, carrierName: String, grid: Int, site: Int): Option[String] = {
		def ok[A](a: A, b: A): Boolean = (a == null || a == b)
		def okInt(a: Integer, b: Int): Boolean = (a == null || a == b)
		def matches(siteBean: SiteBean): Boolean =
			ok(siteBean.carrier, carrierName) &&
			okInt(siteBean.grid, grid) &&
			okInt(siteBean.site, site)
		for ((ident, bean) <- tableSetupBean.sites.toMap) {
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
			(siteE, site) <- createSite(carrierE, site_i, s"${agentIdent} device ${carrierE.sName} site ${site_i+1}").toList
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
		val siteIdent = sc.site.label.get
		val logic1 = Rel("device-can-site", List(dc.deviceIdent, sc.site.label.get))
		val logic2_l = sc.labwareModel_l.map(m => Rel("device-can-model", List(dc.deviceIdent, eb.getIdent(m).toOption.get)))
		RsSuccess(logic1 :: logic2_l)
	}
	
	private def loadTransporters(
		agent: Agent,
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
		
		// Create transporters
		val roma_m = new HashMap[Int, Transporter]()
		
		{
			// List of RoMa indexes
			val roma_li = carrierData.mapCarrierToVectors.values.flatMap(_.map(_.iRoma)).toSet
			// Add transporter device for each RoMa
			for (roma_i <- roma_li) {
				val ident = s"${agentIdent}__transporter${roma_i + 1}"
				val roma = Transporter(gid)
				identToAgentObject(ident) = roma_i.asInstanceOf[Integer]
				roma_m(roma_i) = roma
				eb.addDevice(agent, roma, ident)
			}
		}
		
		// Create transporter specs
		// Map vector class to transporter spec
		val transporterSpec_m = new HashMap[String, TransporterSpec]()
		
		{
			val vectorClass_l: List[String] = carrierData.mapCarrierToVectors.toList.flatMap(_._2).map(_.sClass).toSet.toList.sorted
			var vector_i = 0
			for (vectorClass <- vectorClass_l) {
				val spec = TransporterSpec(gid, Some(s"${agentIdent} ${vectorClass}"))
				val ident = s"${agentIdent}__transporterSpec${vector_i}"
				identToAgentObject(ident) = vectorClass
				//println(ident)
				transporterSpec_m(vectorClass) = spec
				for (roma <- roma_m.values) {
					vector_i += 1
					eb.addDeviceSpec(roma, spec, ident)
				}
			}
		}
		
		// Find which sites the transporters can access
		val agentRomaVectorToSite_m: Map[(String, String, String), List[Site]] = {
			val transporterBlacklist_l = {
				if (agentBean.transporterBlacklist == null)
					Nil
				else
					agentBean.transporterBlacklist.toList
			}
			def isBlacklisted(roma: Int, vector: String, siteIdent: String): Boolean = {
				transporterBlacklist_l.exists { item =>
					val romaMatches = item.roma == null || item.roma == roma
					val vectorMatches = item.vector == null || item.vector == vector
					val siteMatches = item.site == null || item.site == siteIdent
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
				val transporter = roma_m(vector.iRoma)
				val deviceIdent = eb.entityToIdent_m(transporter)
				val siteIdent = eb.entityToIdent_m(site)
				val spec = transporterSpec_m(vector.sClass)
				val key = (agentIdent, deviceIdent, vector.sClass)
				if (isBlacklisted(vector.iRoma + 1, vector.sClass, siteIdent)) {
					logger.info(s"blacklisted vector $key for site $siteIdent")
				}
				else {
					agentRomaVectorToSite_m(key) = site :: agentRomaVectorToSite_m.getOrElse(key, Nil)
					eb.addRel(Rel("transporter-can", List(deviceIdent, eb.entityToIdent_m(site), eb.entityToIdent_m(spec))))
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
				("user", "", "") -> (offsite :: RqResult.flatten(tableSetupBean.userSites.toList.map(eb.getEntityAs[Site])))
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
		deviceIdent_? : Option[String],
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
							val siteIdent = sc.site.label.get
							Rel("device-can-open-site", List(dc.deviceIdent, siteIdent))
						}
						default_l ++ l 
					}
				}
				Some(new DeviceConfigPre(
					"reader",
					device_? = Some(new Reader(gid, Some(carrierE.sName))),
					handler_? = Some(new EvowareInfiniteM200InstructionHandler(carrierE)),
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
							val siteIdent = sc.site.label.get
							Rel("device-can-open-site", List(dc.deviceIdent, siteIdent))
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
					val siteExternalIdent_? = siteExternal_?.map(_._2.label.get)
					
					for {
						siteIdentBase <- siteExternalIdent_?.toList
						siteInternal_i <- List.range(0, 4)
						site = Site(gid, Some(s"${siteIdentBase}_${siteInternal_i+1}"), Some(s"internal site ${siteInternal_i+1}"))
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
							val siteIdent = sc.site.label.get
							Rel("site-closed", List(siteIdent))
						}
						default_l ++ l 
					}
				}

				Some(new DeviceConfigPre(
					"centrifuge",
					device_? = Some(new Centrifuge(gid, Some(carrierE.sName))),
					handler_? = new Some(new EvowareHettichCentrifugeInstructionHandler(carrierE)),
					overrideCreateSites_? = Some(overrideCreateSites),
					overrideSiteLogic_? = Some(overrideSiteLogic)
				))
				// TODO: Add operators for opening the sites and for closing the device
				
			case _ =>
				None
		}
	}
	
	def loadSealerPrograms(): RsResult[List[EvowareSealerProgram]] = {
		for {
			sealerProgram_l <- {
				if (agentBean.sealerProgram != null) {
					for {
						sealerProgram_l <- RsResult.mapFirst(agentBean.sealerProgram.toList) { bean =>
							for {
								_ <- RsResult.assert(bean.model != null, "`model` parameter missing: a labware model must be supplied for the sealer program")
								_ <- RsResult.assert(bean.filename != null, "`filename` parameter missing: a filename must be supplied for the sealer program")
								model <- eb.getEntityAs[LabwareModel](bean.model)
							} yield {
								EvowareSealerProgram(model, bean.filename)
							}
						}
					} yield sealerProgram_l
				}
				else RsSuccess(List())
			}
		} yield sealerProgram_l
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
			deviceIdent: String,
			carrierE: roboliq.evoware.parser.Carrier
		) {
			// Add device
			eb.addDevice(agent, device, deviceIdent)
			identToAgentObject(deviceIdent) = carrierE
		}
		
		def addDeviceAndSitesAndModels(
			device: Device,
			deviceIdent: String,
			carrierE: roboliq.evoware.parser.Carrier
		) {
			addDeviceOnly(device, deviceIdent, carrierE)
	
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
		
		def createDeviceIdent(carrierE: roboliq.evoware.parser.Carrier): String = {
			agentIdent + "__" + carrierE.sName.map(c => if (c.isLetterOrDigit) c else '_')
		}
		
		//println("Carriers: " + tableData.mapCarrierToGrid.keys.mkString("\n"))
		for ((carrierE, iGrid) <- tableData.mapCarrierToGrid) {
			//println(s"carrier: $iGrid, ${carrierE.sName}, ${carrierE.partNo_?}")
			carrierE.partNo_?.getOrElse(carrierE.sName) match {
				// Infinite M200
				case "Tecan part no. 30016056 or 30029757" =>
					val deviceIdent = createDeviceIdent(carrierE)
					val handler = new EvowareInfiniteM200InstructionHandler(carrierE)
					val device = new Reader(gid, Some(carrierE.sName))
					
					eb.addDevice(agent, device, deviceIdent)
					// Bind deviceIdent to the handler instead of to the carrier
					identToAgentObject(deviceIdent) = handler
					// Add device sites
					getDeviceSitesAndModels(carrierE).foreach { case (site, model_l) =>
						val siteIdent = eb.getIdent(site).toOption.get
						eb.addDeviceSite(device, site)
						model_l.foreach(m => eb.addDeviceModel(device, m))
						eb.addRel(Rel("device-can-open-site", List(deviceIdent, siteIdent)))
					}

				case "MP 2Pos H+P Shake" =>
					val deviceIdent = createDeviceIdent(carrierE)
					// REFACTOR: duplicates addShaker(), because for this device, only the second site can actually be used for shaking
					val device = new Shaker(gid, Some(carrierE.sName))
					// Add device
					eb.addDevice(agent, device, deviceIdent)
					identToAgentObject(deviceIdent) = carrierE
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
					val deviceIdent = createDeviceIdent(carrierE)
					val device = addPeeler(deviceIdent, carrierE)
					// Add user-defined specs for this device
					for ((deviceIdent2, plateModelIdent, specIdent) <- deviceToModelToSpec_l if deviceIdent2 == deviceIdent) {
						// Get or create the sealer spec for specIdent
						val spec: PeelerSpec = eb.getEntity(specIdent) match {
							case Some(spec) => spec.asInstanceOf[PeelerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specIdent).get._2
								identToAgentObject(specIdent) = internal
								PeelerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specIdent)
						// Let entity base know that that the spec can be used for the plate model
						eb.addRel(Rel("device-spec-can-model", List(deviceIdent, specIdent, plateModelIdent)))
					}
					
				case "RoboSeal" =>
					val deviceIdent = createDeviceIdent(carrierE)
					val device = addSealer(deviceIdent, carrierE)
					// Add user-defined specs for this device
					for ((deviceIdent2, plateModelIdent, specIdent) <- deviceToModelToSpec_l if deviceIdent2 == deviceIdent) {
						// Get or create the sealer spec for specIdent
						val spec: SealerSpec = eb.getEntity(specIdent) match {
							case Some(spec) => spec.asInstanceOf[SealerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specIdent).get._2
								identToAgentObject(specIdent) = internal
								SealerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specIdent)
						// Let entity base know that that the spec can be used for the plate model
						eb.addRel(Rel("device-spec-can-model", List(deviceIdent, specIdent, plateModelIdent)))
					}
					
				// Te-Shake 2Pos
				case "Tecan part no. 10760722 with 10760725" =>
					val deviceIdent = createDeviceIdent(carrierE)
					val device = new Shaker(gid, Some(carrierE.sName))
					addDeviceAndSitesAndModels(device, deviceIdent, carrierE)
					// Add user-defined specs for this device
					for ((deviceIdent2, specIdent) <- deviceToSpec_l if deviceIdent2 == deviceIdent) {
						// Get or create the spec for specIdent
						val spec: ShakerSpec = eb.getEntity(specIdent) match {
							case Some(spec) => spec.asInstanceOf[ShakerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specIdent).get._2
								identToAgentObject(specIdent) = internal
								ShakerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specIdent)
					}
					
				case "TRobot1" =>
					val deviceIdent = createDeviceIdent(carrierE)
					val device = new Thermocycler(gid, Some(carrierE.sName))
					addDeviceAndSitesAndModels(device, deviceIdent, carrierE)
					// Add user-defined specs for this device
					for ((deviceIdent2, specIdent) <- deviceToSpec_l if deviceIdent2 == deviceIdent) {
						// Get or create the spec for specIdent
						val spec: ThermocyclerSpec = eb.getEntity(specIdent) match {
							case Some(spec) => spec.asInstanceOf[ThermocyclerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specIdent).get._2
								identToAgentObject(specIdent) = internal
								ThermocyclerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specIdent)
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
				for ((deviceIdent2, specIdent) <- deviceToSpec_l if deviceIdent2 == deviceIdent) {
					// Get or create the spec for specIdent
					val spec: ShakerSpec = eb.getEntity(specIdent) match {
						case Some(spec) => spec.asInstanceOf[ShakerSpec]
						case None =>
							// Store the evoware string for this spec
							val internal = specToString_l.find(_._1 == specIdent).get._2
							identToAgentObject(specIdent) = internal
							ShakerSpec(gid, None, Some(internal))
					}
					// Register the spec
					eb.addDeviceSpec(device, spec, specIdent)
				}
	}
	*/

	/* TODO: need to handle thermocycler programs
				// Add user-defined specs for this device
				for ((deviceIdent2, specIdent) <- deviceToSpec_l if deviceIdent2 == deviceIdent) {
					// Get or create the spec for specIdent
					val spec: ThermocyclerSpec = eb.getEntity(specIdent) match {
						case Some(spec) => spec.asInstanceOf[ThermocyclerSpec]
						case None =>
							// Store the evoware string for this spec
							val internal = specToString_l.find(_._1 == specIdent).get._2
							identToAgentObject(specIdent) = internal
							ThermocyclerSpec(gid, None, Some(internal))
					}
					// Register the spec
					eb.addDeviceSpec(device, spec, specIdent)
				}
	*/
}