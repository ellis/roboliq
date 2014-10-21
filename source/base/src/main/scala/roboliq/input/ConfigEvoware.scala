package roboliq.input

import scala.collection.JavaConversions._
import roboliq.evoware.translator.EvowareSealerProgram
import scala.collection.mutable.ArrayBuffer
import scalax.collection.edge.LkUnDiEdge
import roboliq.evoware.translator.EvowareInfiniteM200InstructionHandler
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
import roboliq.evoware.translator.EvowareDeviceInstructionHandler
import roboliq.evoware.parser.CarrierSite
import roboliq.evoware.parser.Carrier
import roboliq.entities.Entity
import roboliq.core.RsError
import roboliq.entities.Centrifuge

/**
 * @param postProcess_? An option function to call after all sites have been created, which can be used for further handling of device configuration.
 */
private case class DeviceConfigPre(
	val typeName: String,
	val deviceIdent_? : Option[String] = None,
	val device_? : Option[Device] = None,
	val handler_? : Option[EvowareDeviceInstructionHandler] = None,
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
			(labwareModel_l, tip_l, modelEToModel_l) = tuple1
			
			siteIdToSiteAndModels_m <- loadSites(agent, pipetterIdent, modelEToModel_l)
			siteIdToSite_m = siteIdToSiteAndModels_m.mapValues(_._1)
			_ <- loadTransporters(agent, siteIdToSite_m)
			_ <- loadDevices(agent, siteIdToSiteAndModels_m)
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
		List[LabwareModelBean],
		List[Tip],
		List[(EvowareLabwareModel, LabwareModel)]
	)] = {
		val labwareModel_l = if (agentBean.labwareModels == null) Nil else agentBean.labwareModels.toList

		// Tip models
		for {
			tipModel_l <- loadAgentTipModels()
			tip_l <- loadAgentTips(tipModel_l)
			modelEToModel_l <- loadAgentPlateModels(labwareModel_l)
		} yield (labwareModel_l, tip_l, modelEToModel_l)
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
		val labwareModelE0_l = carrierData.models.collect({case m: roboliq.evoware.parser.EvowareLabwareModel if labwareNamesOfInterest_l.contains(m.sName) || labwareNamesOfInterest_l.contains(m.sName.replace(" portrait", "")) => m})
		val evowareNameToLabwareModel_m = agentBean.labwareModels.toList.map(x => x.evowareName -> x).toMap
		// Only keep the ones we have LabwareModels for (TODO: seems like a silly step -- should probably filter labwareNamesOfInterest_l to begin with)
		val labwareModelE_l = labwareModelE0_l.filter(mE => evowareNameToLabwareModel_m.contains(mE.sName))
		for {
			modelEToModel_l <- RsResult.mapAll(labwareModelE_l) { mE => 
				for {
					model <- RsResult.from(evowareNameToLabwareModel_m.get(mE.sName), s"evoware labware model not found: `${mE.sName}`")
				} yield {
					//println("mE.sName: "+mE.sName)
					//if (mE.sName.contains("Plate") || mE.sName.contains("96") || mE.sName.contains("Trough")) {
					val m = PlateModel(model.name, Option(model.label), Some(mE.sName), mE.nRows, mE.nCols, LiquidVolume.ul(mE.ul))
					//idToModel_m(mE.sName) = m
					eb.addModel(m, model.name)
					// All models can be offsite
					eb.addStackable(offsiteModel, m)
					// The user arm can handle all models
					eb.addDeviceModel(userArm, m)
					//eb.addRel(Rel("transporter-can", List(eb.names(userArm), eb.names(m), "nil")))
					identToAgentObject(model.name) = mE
					(mE, m)
				}
			}
		} yield modelEToModel_l
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
	
	/**
	 * @returns Map from SiteID to its Site and the LabwareModels it can accept
	 */
	private def loadSites(
		agent: Agent,
		pipetterIdent: String,
		modelEToModel_l: List[(EvowareLabwareModel, LabwareModel)]
		//labwareModelE_l: List[roboliq.evoware.parser.EvowareLabwareModel],
		//idToModel_m: HashMap[String, LabwareModel]
	): RsResult[Map[(Int, Int), (Site, Set[LabwareModel])]] = {
		val siteIdToSite_m = new HashMap[(Int, Int), Site]
		val carriersSeen_l = new HashSet[Int]

		def createSite(carrierE: roboliq.evoware.parser.Carrier, site_i: Int, description: String): Option[(CarrierSite, Site)] = {
			val grid_i = tableData.mapCarrierToGrid(carrierE)
			// TODO: should adapt CarrierSite to require grid_i as a parameter 
			findSiteIdent(tableSetupBean, carrierE.sName, grid_i, site_i + 1).map { siteIdent =>
				val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
				val site = Site(gid, Some(siteIdent), Some(description))
				(siteE, site)
			}
		}

		def createDeviceIdent(carrierE: Carrier): String = {
			agentIdent + "__" + carrierE.sName.map(c => if (c.isLetterOrDigit) c else '_')
		}

		def addSite(carrierE: roboliq.evoware.parser.Carrier, site_i: Int, description: String) {
			val grid_i = tableData.mapCarrierToGrid(carrierE)
			// TODO: should adapt CarrierSite to require grid_i as a parameter 
			val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
			val siteId = (carrierE.id, site_i)
			findSiteIdent(tableSetupBean, carrierE.sName, grid_i, site_i + 1).foreach { siteIdent =>
				val site = Site(gid, Some(siteIdent), Some(description))
				siteIdToSite_m(siteId) = site
				identToAgentObject(siteIdent) = siteE
				eb.addSite(site, siteIdent)
			}
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
		
		// Create Device and their Sites
		val deviceConfig_l = for {
			o <- tableData.lExternalObject if !carriersSeen_l.contains(o.carrier.id)
			carrierE = o.carrier
			_ = carriersSeen_l += carrierE.id
			dcp <- loadDevice(carrierE).toList
		} yield {
			val deviceIdent: String = dcp.deviceIdent_?.getOrElse(createDeviceIdent(carrierE))
			val device = dcp.device_?.getOrElse(new Device { val key = gid; val label = Some(carrierE.sName); val description = None; val typeNames = List(dcp.typeName) })
			val siteConfig_l = for {
				site_i <- List.range(0, carrierE.nSites)
				(siteE, site) <- createSite(carrierE, site_i, s"${agentIdent} device ${carrierE.sName} site ${site_i+1}").toList
				siteId = (carrierE.id, site_i)
				model_l <- siteIdToModels_m.get(siteId).toList
			} yield {
				DeviceSiteConfig(siteE, site, model_l.toList)
			}
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
		
		// TODO: For tubes, create their on-bench Sites and SiteModels
		// TODO: Let userArm handle tube models
		// TODO: Let userArm access all sites that the robot arms can't
		
		// Create SiteModels for for sites which hold Plates
		{
			// Find all unique sets of labware models
			val unique = siteIdToModels_m.values.toSet
			val modelsToSiteModel_m = new HashMap[Set[LabwareModel], SiteModel]
			var i = 1
			for (l <- unique) {
				val sm = SiteModel(l.toString)
				modelsToSiteModel_m(l) = sm
				eb.addModel(sm, f"sm${i}")
				eb.addStackables(sm, l.toList)
				i += 1
			}

			// Assign SiteModels to Sites
			for ((siteId, l) <- siteIdToModels_m) {
				val site = siteIdToSite_m(siteId)
				val sm = modelsToSiteModel_m(l)
				eb.setModel(site, sm)
			}
		}
		
		// Update EntityBase with sites and logic
		val siteDevice_l = deviceConfig_l.flatMap(_.siteConfig_l.map(sc => (sc.siteE, sc.site)))
		val site_l = siteHotel_l ++ siteDevice_l ++ siteBench_l
		for ((siteE, site) <- site_l) {
			val siteIdent = site.label.get
			val siteId = (siteE.carrier.id, siteE.iSite)
			siteIdToSite_m(siteId) = site
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
		
		val siteIdToSiteAndModels_m = siteIdToSite_m.toMap.map { case (siteId, site) =>
			(siteId, (site, siteIdToModels_m.get(siteId).map(_.toSet).getOrElse(Set())))
		}
		RsSuccess(siteIdToSiteAndModels_m)
	}

	def getDefaultSitesLogic(dc: DeviceConfig): RsResult[List[Rel]] = {
		for {
			ll <- RsResult.mapAll(dc.siteConfig_l){ sc => getDefaultSiteLogic(dc, sc) }
		} yield ll.flatten
	}
	
	def getDefaultSiteLogic(dc: DeviceConfig, sc: DeviceSiteConfig): RsResult[List[Rel]] = {
		val siteIdent = sc.site.label.get
		val logic1 = Rel("device-can-site", List(dc.deviceIdent, sc.site.label.get))
		val logic2_l = sc.labwareModel_l.map(m => Rel("device-can-model", List(dc.deviceIdent, eb.getIdent(m).toOption.get)))
		RsSuccess(logic1 :: logic2_l)
	}
	
	private def loadTransporters(
		agent: Agent,
		siteIdToSite_m: Map[(Int, Int), Site]
	): RqResult[Unit] = {
		
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
		
		val agentRomaVectorToSite_m = new HashMap[(String, String, String), List[Site]]
		// Find which sites the transporters can access
		for ((carrierE, vector_l) <- carrierData.mapCarrierToVectors) {
			for (site_i <- 0 until carrierE.nSites) {
				val siteId = (carrierE.id, site_i)
				siteIdToSite_m.get(siteId).foreach { site =>
					for (vector <- vector_l) {
						val transporter = roma_m(vector.iRoma)
						val deviceIdent = eb.entityToIdent_m(transporter)
						val spec = transporterSpec_m(vector.sClass)
						val key = (agentIdent, deviceIdent, vector.sClass)
						agentRomaVectorToSite_m(key) = site :: agentRomaVectorToSite_m.getOrElse(key, Nil)
						eb.addRel(Rel("transporter-can", List(deviceIdent, eb.entityToIdent_m(site), eb.entityToIdent_m(spec))))
					}
				}
			}
		}

		val graph = {
			// Add user-accessible sites into the graph
			val offsite = eb.getEntityAs[Site]("offsite").getOrElse(null)
			agentRomaVectorToSite_m(("user", "", "")) = offsite :: RqResult.flatten(tableSetupBean.userSites.toList.map(eb.getEntityAs[Site]))
			// Populate graph from entries in test_m
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
		//println("graph: "+graph.size)
		//graph.take(5).foreach(println)
		//graph.foreach(println)
		RsSuccess(())
	}
	
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
	
	/**
	 * TODO: most of this should be loaded from 
	 */
	private def loadDevice(carrierE: Carrier): Option[DeviceConfigPre] = {
		/*typeName: String,
		deviceIdent_? : Option[String],
		device_? : Option[Device],
		handler_? : Option[EvowareDeviceInstructionHandler],
		site_l_? : Option[List[Site]]*/
		carrierE.partNo_?.getOrElse(carrierE.sName) match {
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
					"peeler",
					device_? = Some(new Peeler(gid, Some(carrierE.sName)))
				))
				
			// Te-Shake 2Pos
			case "Tecan part no. 10760722 with 10760725" =>
				Some(new DeviceConfigPre(
					"shaker",
					device_? = Some(new Shaker(gid, Some(carrierE.sName)))
				))
				
			case "TRobot1" =>
				Some(new DeviceConfigPre(
					"thermocycler",
					device_? = Some(new Thermocycler(gid, Some(carrierE.sName)))
				))
			
			case "Hettich Centrifuge" => // TODO: FIXME: Get the correct ID or name
				Some(new DeviceConfigPre(
					"centrifuge",
					device_? = Some(new Centrifuge(gid, Some(carrierE.sName)))
				))
				// TODO: Add 4 sites, aliasing them to the device site's (grid,site)
				// TODO: Remove the device site from 'eb' (probably necessary to prevent it from being added in the first place)
				// TODO: Add operators for opening the sites and for closing the device
				//...
				
				
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