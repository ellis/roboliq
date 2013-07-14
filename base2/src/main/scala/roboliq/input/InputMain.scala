package roboliq.input

import spray.json.JsObject
import spray.json.JsString
import roboliq.entities.Entity
import scala.collection.mutable.ArrayBuffer
import roboliq.entities._
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap

class PlateBean {
	var name: String = null
	var model: String = null
}

case class PlateInput(
	name: Option[String],
	model: Option[String]
)

case class TubeInput(
	name: Option[String],
	model: Option[String],
	contents: Option[String]
)

object InputMain extends App {
	
	val eb = new EntityBase
	
	val plateInputs = List[Map[String, String]](
		Map("name" -> "plate1", "model" -> "Thermocycler Plate", "location" -> "offsite")
	)
	val tubeInputs = List[Map[String, String]](
		//Map("name" -> "tube1", "model" -> "Small Tube", "contents" -> "water")
	)
	val protocol = List[JsObject](
		JsObject(
			"command" -> JsString("shake"),
			"object" -> JsString("plate1")
		), 
		JsObject(
			"command" -> JsString("seak"),
			"object" -> JsString("plate1")
		)
	)

	var tasks = new ArrayBuffer[Rel]
	var var_i = 0

	def gid: String = java.util.UUID.randomUUID().toString()
	def nvar: Int = { var_i += 1; var_i }
	
	setup2()
	
	for (m <- plateInputs) {
		val id = m.getOrElse("id", gid)
		val name = m.getOrElse("name", id)
		val modelKey = m("model")
		println("modelKey: "+modelKey)
		println("eb.nameToEntity: "+eb.nameToEntity)
		println("eb.idToEntity: "+eb.idToEntity)
		println("eb.idToEntity.get(\"Thermocycler Plate\"): "+eb.idToEntity.get("Thermocycler Plate"))
		val model = eb.getEntity(modelKey).get.asInstanceOf[PlateModel]
		val plate = new Plate(id)
		eb.addLabware(plate, name)
		eb.setModel(plate, model)
		m.get("location") match {
			case Some(key) =>
				val entity = eb.getEntity(key).get
				eb.setLocation(plate, entity)
			case _ =>
		}
	}
	
	for (m <- tubeInputs) {
		val id = m.getOrElse("id", gid)
		val name = m.getOrElse("name", id)
		val modelKey = m("model")
		val model = eb.getEntity(modelKey).get.asInstanceOf[LabwareModel]
		val tube = new Plate(id)
		eb.addLabware(tube, name)
		eb.setModel(tube, model)
	}
	
	for (js <- protocol) {
		if (js.fields.contains("command")) {
			js.fields.get("command") match {
				case Some(JsString("shake")) =>
					js.fields.get("object") match {
						case Some(JsString(key)) =>
							val agent = f"?a$nvar%04d"
							val device = f"?d$nvar%04d"
							val plate = eb.getEntity(key).get.asInstanceOf[Labware]
							val plateName = eb.names(plate)
							val model = eb.labwareToModel_m(plate)
							val modelName = eb.names(model)
							tasks += Rel("sealer-run", List(agent, device, plateName, modelName, f"?s$nvar%04d"))
						case _ =>
					}
				case Some(JsString("seal")) =>
				case _ =>
			}
		}
	}
	
	def setup() {
		import roboliq.entities._
		
		val user = Agent(gid)
		val r1 = Agent(gid)
		val userArm = Transporter(gid)
		val r1arm = Transporter(gid)
		val pipetter = Pipetter(gid)
		val sealer = Sealer(gid)
		val shaker = Shaker(gid)
		val thermocycler = Thermocycler(gid)
		val siteModelAll = SiteModel(gid)
		val siteModel1 = SiteModel(gid)
		val siteModel12 = SiteModel(gid)
		val offsite = Site(gid)
		val s1 = Site(gid)
		val s2 = Site(gid)
		val sealerSite = Site(gid)
		val shakerSite = Site(gid)
		val thermocyclerSite = Site(gid)
		val m1 = PlateModel("Thermocycler Plate", 8, 12, LiquidVolume.ul(100))
		val m2 = PlateModel("Deep Plate", 8, 12, LiquidVolume.ul(500))
		val shakerSpec1 = ShakerSpec(gid)
		val thermocyclerSpec1 = ThermocyclerSpec(gid)
		
		eb.addAgent(user, "user")
		eb.addAgent(r1, "r1")
		eb.addModel(siteModelAll, "siteModelAll")
		eb.addModel(siteModel1, "siteModel1")
		eb.addModel(siteModel12, "siteModel12")
		eb.addModel(m1, "m1")
		eb.addModel(m2, "m2")
		eb.addSite(offsite, "offsite")
		eb.addSite(s1, "s1")
		eb.addSite(s2, "s2")
		eb.addSite(sealerSite, "sealerSite")
		eb.addSite(shakerSite, "shakerSite")
		eb.addSite(thermocyclerSite, "thermocyclerSite")
		eb.addDevice(user, userArm, "userArm")
		eb.addDevice(r1, r1arm, "r1arm")
		eb.addDevice(r1, pipetter, "pipetter")
		eb.addDevice(r1, sealer, "sealer")
		eb.addDevice(r1, shaker, "shaker")
		eb.addDevice(r1, thermocycler, "thermocycler")
		eb.addDeviceModels(userArm, List(m1, m2))
		eb.addDeviceModels(r1arm, List(m1, m2))
		eb.addDeviceModels(pipetter, List(m1, m2))
		eb.addDeviceModels(sealer, List(m1))
		eb.addDeviceModels(shaker, List(m1, m2))
		eb.addDeviceModels(thermocycler, List(m1))
		eb.addDeviceSites(userArm, List(offsite, s1))
		eb.addDeviceSites(r1arm, List(s1, s2, sealerSite, thermocyclerSite))
		eb.addDeviceSites(pipetter, List(s1, s2))
		eb.addDeviceSites(sealer, List(sealerSite))
		eb.addDeviceSites(shaker, List(shakerSite))
		eb.addDeviceSites(thermocycler, List(thermocyclerSite))
		eb.addDeviceSpec(shaker, shakerSpec1, "shakerSpec1")
		eb.addDeviceSpec(thermocycler, thermocyclerSpec1, "thermocyclerSpec1")
		eb.addStackables(siteModelAll, List(m1, m2))
		eb.addStackables(siteModel1, List(m1))
		eb.addStackables(siteModel12, List(m1, m2))
		eb.setModel(offsite, siteModelAll)
		eb.setModel(s1, siteModel12)
		eb.setModel(s2, siteModel12)
		eb.setModel(sealerSite, siteModel1)
		eb.setModel(shakerSite, siteModel12)
		eb.setModel(thermocyclerSite, siteModel1)
	}
	
	/**
	 * Challenges when reading in Evoware configuration files:
	 * 
	 * There are lots of labware models we don't want to use, so we need to filter them out somehow.
	 * A carrier may have mutually exclusive sites, so in such cases, we need to filter out the ones that can't be used on the current table.
	 * When a table has tube labware on which cannot be moved by the RoMa, that labware should be treated as a site for tubes.
	 * What to do with other labware on the table definition?  One thing we should probably do is add it to this list of labware models we're interested in.
	 * We want to use site models, but these are not declared in Evoware, so we'll need to extract them indirectly. 
	 */
	def setup2() {
		import roboliq.entities._
		
		val user = Agent(gid)
		val r1 = Agent(gid)
		val userArm = Transporter(gid)
		val pipetter = Pipetter(gid)
		val shaker = Shaker(gid)
		val thermocycler = Thermocycler(gid)
		val siteModelAll = SiteModel(gid)
		val siteModel1 = SiteModel(gid)
		val siteModel12 = SiteModel(gid)
		val offsite = Site(gid)
		val s1 = Site(gid)
		val s2 = Site(gid)
		val shakerSite = Site(gid)
		val thermocyclerSite = Site(gid)
		val m1 = PlateModel("Thermocycler Plate", 8, 12, LiquidVolume.ul(100))
		val m2 = PlateModel("Deep Plate", 8, 12, LiquidVolume.ul(500))
		val shakerSpec1 = ShakerSpec(gid)
		val thermocyclerSpec1 = ThermocyclerSpec(gid)
		
		eb.addAgent(user, "user")
		eb.addModel(siteModelAll, "siteModelAll")
		eb.addModel(siteModel1, "siteModel1")
		eb.addModel(siteModel12, "siteModel12")
		eb.addModel(m1, "m1")
		eb.addModel(m2, "m2")
		eb.addSite(offsite, "offsite")
		eb.addSite(s1, "s1")
		eb.addSite(s2, "s2")
		eb.addSite(shakerSite, "shakerSite")
		eb.addSite(thermocyclerSite, "thermocyclerSite")
		eb.addDevice(user, userArm, "userArm")
		
		val labwareNamesOfInterest_l = new HashSet[String]
		labwareNamesOfInterest_l += "D-BSSE 96 Well PCR Plate"
		
		/*eb.addDevice(r1, pipetter, "pipetter")
		eb.addDevice(r1, shaker, "shaker")
		eb.addDevice(r1, thermocycler, "thermocycler")
		eb.addDeviceModels(userArm, List(m1, m2))
		//eb.addDeviceModels(r1arm, List(m1, m2))
		eb.addDeviceModels(pipetter, List(m1, m2))
		//eb.addDeviceModels(sealer, List(m1))
		eb.addDeviceModels(shaker, List(m1, m2))
		eb.addDeviceModels(thermocycler, List(m1))
		eb.addDeviceSites(userArm, List(offsite, s1))
		//eb.addDeviceSites(r1arm, List(s1, s2, thermocyclerSite))
		eb.addDeviceSites(pipetter, List(s1, s2))
		eb.addDeviceSites(shaker, List(shakerSite))
		eb.addDeviceSites(thermocycler, List(thermocyclerSite))
		eb.addDeviceSpec(shaker, shakerSpec1, "shakerSpec1")
		eb.addDeviceSpec(thermocycler, thermocyclerSpec1, "thermocyclerSpec1")
		eb.addStackables(siteModelAll, List(m1, m2))
		eb.addStackables(siteModel1, List(m1))
		eb.addStackables(siteModel12, List(m1, m2))
		eb.setModel(offsite, siteModelAll)
		eb.setModel(s1, siteModel12)
		eb.setModel(s2, siteModel12)
		eb.setModel(shakerSite, siteModel12)
		eb.setModel(thermocyclerSite, siteModel1)*/

		for {
			carrier <- roboliq.evoware.parser.EvowareCarrierData.loadFile("./testdata/bsse-robot1/config/carrier.cfg")
			table <- roboliq.evoware.parser.EvowareTableData.loadFile(carrier, "./testdata/bsse-robot1/config/table-01.esc")
		} {
			eb.addAgent(r1, "r1")

			// Add labware on the table definition to the list of labware we're interested in
			labwareNamesOfInterest_l ++= table.mapSiteToLabwareModel.values.map(_.sName)

			// Create PlateModels
			val labwareModelEs = carrier.models.collect({case m: roboliq.evoware.parser.LabwareModel if labwareNamesOfInterest_l.contains(m.sName) => m})
			val idToModel_m = new HashMap[String, LabwareModel]
			for (mE <- labwareModelEs) {
				if (mE.sName.contains("Plate") || mE.sName.contains("96")) {
					val m = PlateModel(mE.sName, mE.nRows, mE.nCols, LiquidVolume.ul(mE.ul))
					idToModel_m(mE.sName) = m
					eb.addModel(m, f"m${idToModel_m.size}%03d")
				}
			}
			
			// Create Sites
			val siteEsToSiteModel_m = new HashMap[List[(Int, Int)], SiteModel]
			val siteIdToSite_m = new HashMap[(Int, Int), Site]
			//val siteEToSite_m = new HashMap[roboliq.evoware.parser.CarrierSite, Site]
			val carriersSeen_l = new HashSet[Int]
			// Create Hotel Sites
			for (o <- table.lHotelObject) {
				val carrierE = o.parent
				carriersSeen_l += carrierE.id
				for (site_i <- 0 until carrierE.nSites) {
					val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
					val siteId = (carrierE.id, site_i)
					val site = Site(s"hotel_${carrierE.id}x${site_i+1}")
					siteIdToSite_m(siteId) = site
					//eb.addSite(site, site.id)
				}
			}
			
			// Create Device Sites
			for (o <- table.lExternalObject if !carriersSeen_l.contains(o.carrier.id)) {
				val carrierE = o.carrier
				carriersSeen_l += carrierE.id
				for (site_i <- 0 until carrierE.nSites) {
					val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
					val siteId = (carrierE.id, site_i)
					val site = Site(s"device_${carrierE.id}x${site_i+1}")
					siteIdToSite_m(siteId) = site
					//eb.addSite(site, carrierE.sName+site.id)
				}
			}
			
			// Create on-bench Sites
			for ((carrierE, grid_i) <- table.mapCarrierToGrid if !carriersSeen_l.contains(carrierE.id)) {
				for (site_i <- 0 until carrierE.nSites) {
					val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
					val siteId = (carrierE.id, site_i)
					val site = Site(siteId.toString)
					siteIdToSite_m(siteId) = site
					eb.addSite(site, f"bench_${grid_i}%03dx${site_i+1}")
					/*
					// if the site has labware on it, then limit labwareModels to that model
					val filtered = labwareModelEs.filter(mE => idToModel_m.contains(mE.sName) && mE.sites.contains(siteId))
					for (modelE <- ) {
						val model = idToModel_m.get(modelE.sName) match {
							case Some(model) => model
							case None =>
								val model = PlateModel(modelE.sName, modelE.nRows, modelE.nCols, LiquidVolume.ul(modelE.ul))
								idToModel_m(model.id) = model
								eb.addModel(model, f"m${idToModel_m.size}%03d")
								model
						}
						eb.addStackable(siteModel, model)
					}
					*/
				}
			}
			
			// Create SiteModels
			{
				// First gather map of all relevant labware models that can be placed on each site 
				val siteIdToModel_m = new HashMap[(Int, Int), collection.mutable.Set[LabwareModel]] with MultiMap[(Int, Int), LabwareModel]
				for (mE <- labwareModelEs if idToModel_m.contains(mE.sName)) {
					val m = idToModel_m(mE.sName)
					for (siteId <- mE.sites if siteIdToSite_m.contains(siteId)) {
						val site = siteIdToSite_m(siteId)
						siteIdToModel_m.addBinding(siteId, m)
					}
				}
				// Find all unique sets of labware models
				val unique = siteIdToModel_m.values.toSet
				var i = 1
				for (l <- unique) {
					val m = SiteModel(l.toString)
					eb.addModel(m, f"sm${i}")
					i += 1
				}
			}
			
			// Assign SiteModels to Sites

			/*
			// Create Sites and SiteModels
			for ((carrierE, grid_i) <- table.mapCarrierToGrid) {
				for (site_i <- 0 until carrierE.nSites) {
					val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
					val siteId = (carrierE.id, site_i)
					// if the site has labware on it, then limit labwareModels to that model
					val filtered = labwareModelEs.filter(mE => idToModel_m.contains(mE.sName) && mE.sites.contains(siteId))
					for (modelE <- ) {
						val model = idToModel_m.get(modelE.sName) match {
							case Some(model) => model
							case None =>
								val model = PlateModel(modelE.sName, modelE.nRows, modelE.nCols, LiquidVolume.ul(modelE.ul))
								idToModel_m(model.id) = model
								eb.addModel(model, f"m${idToModel_m.size}%03d")
								model
						}
						eb.addStackable(siteModel, model)
					}
				}
			}
			
			
			// List of RoMa indexes
			val roma_li = carrier.mapCarrierToVectors.values.flatMap(_.map(_.iRoma)).toSet
			val roma_m = new HashMap[Int, Transporter]
			// Add transporter device for each RoMa
			for (roma_i <- roma_li) {
				val roma = Transporter(s"RoMa${roma_i + 1}")
				roma_m(roma_i) = roma
				eb.addDevice(r1, roma, s"r1arm${roma_i + 1}")
			}

			table.mapCarrierToGrid.keys.toList.map(_.sName).sorted.foreach(println)
			for ((carrierE, iGrid) <- table.mapCarrierToGrid) {
				carrierE.sName match {
					case "RoboSeal" =>
						addDevice(
							r1,
							"sealer",
							"sealer",
							carrierE,
							iGrid,
							table,
							roma_m,
							labwareModels,
							idToModel_m
						)
					case "RoboPeel" =>
						addDevice(
							r1,
							"peeler",
							"peeler",
							carrierE,
							iGrid,
							table,
							roma_m,
							labwareModels,
							idToModel_m
						)
					case _ =>
						addCarrier(
							carrierE,
							iGrid,
							table,
							roma_m,
							labwareModels,
							idToModel_m
						)
				}
			}
			*/
		}
	}

	def addDevice(
		agent: Agent,
		typeName: String,
		deviceName: String,
		carrierE: roboliq.evoware.parser.Carrier,
		iGrid: Int,
		table: roboliq.evoware.parser.EvowareTableData,
		roma_m: HashMap[Int, Transporter],
		labwareModels: List[roboliq.evoware.parser.LabwareModel],
		idToModel_m: HashMap[String, LabwareModel]
	) {
		val carrierData = table.configFile
		
		// Add device
		val device = new Device { val id = carrierE.sName; val typeNames = List(typeName) }
		eb.addDevice(agent, device, deviceName)

		// Add device sites
		for (site_i <- 0 until carrierE.nSites) {
			// Create site and its model (each site requires its own model)
			val siteModel = SiteModel(gid)
			val site = Site(f"site_${iGrid}%03dx${site_i+1}")
			eb.addModel(siteModel, s"${deviceName}SiteModel${site_i+1}")
			eb.addSite(site, s"${deviceName}Site${site_i+1}")
			eb.setModel(site, siteModel)
			eb.addDeviceSite(device, site)
			// List transporters which can access this site
			// NOTE: SHOULD USE TRANSPORTER SPEC (== Vector)
			for (vector <- carrierData.mapCarrierToVectors.getOrElse(carrierE, Nil)) {
				val roma = roma_m(vector.iRoma)
				eb.addDeviceSite(roma, site)
			}
			// Find the labwares which this site and device accept
			// FIXME: SHOULD USE SEALER SPEC INSTEAD OF addDeviceModel
			val id = (carrierE.id, site_i)
			for (modelE <- labwareModels.filter(_.sites.contains(id))) {
				val model = idToModel_m.get(modelE.sName) match {
					case Some(model) => model
					case None =>
						val model = PlateModel(modelE.sName, modelE.nRows, modelE.nCols, LiquidVolume.ul(modelE.ul))
						idToModel_m(model.id) = model
						eb.addModel(model, f"m${idToModel_m.size}%03d")
						model
				}
				eb.addDeviceModel(device, model)
				eb.addStackable(siteModel, model)
			}
		}
	}

	def addCarrier(
		carrierE: roboliq.evoware.parser.Carrier,
		iGrid: Int,
		table: roboliq.evoware.parser.EvowareTableData,
		roma_m: HashMap[Int, Transporter],
		labwareModels: List[roboliq.evoware.parser.LabwareModel],
		idToModel_m: HashMap[String, LabwareModel]
	) {
		val carrierData = table.configFile
		
		// Add carrier sites
		for (site_i <- 0 until carrierE.nSites) {
			// Create site and its model (each site requires its own model)
			val siteModel = SiteModel(gid)
			val site = Site(f"site_${iGrid}%03dx${site_i+1}")
			eb.addModel(siteModel, f"siteModel_${iGrid}%03dx${site_i+1}")
			eb.addSite(site, f"site_${iGrid}%03dx${site_i+1}")
			eb.setModel(site, siteModel)
			// List transporters which can access this site
			// NOTE: SHOULD USE TRANSPORTER SPEC (== Vector)
			for (vector <- carrierData.mapCarrierToVectors.getOrElse(carrierE, Nil)) {
				val roma = roma_m(vector.iRoma)
				eb.addDeviceSite(roma, site)
			}
			// Find the labwares which this site and device accept
			// FIXME: SHOULD USE SEALER SPEC INSTEAD OF addDeviceModel
			val id = (carrierE.id, site_i)
			for (modelE <- labwareModels.filter(_.sites.contains(id))) {
				val model = idToModel_m.get(modelE.sName) match {
					case Some(model) => model
					case None =>
						val model = PlateModel(modelE.sName, modelE.nRows, modelE.nCols, LiquidVolume.ul(modelE.ul))
						idToModel_m(model.id) = model
						eb.addModel(model, f"m${idToModel_m.size}%03d")
						model
				}
				eb.addStackable(siteModel, model)
			}
		}
	}
	
	println("(defproblem problem domain")
	println(eb.makeInitialConditions)
	println(" ; tasks")
	println(" (")
	tasks.foreach(r => println("  "+r))
	println(" )")
	println(")")
}
