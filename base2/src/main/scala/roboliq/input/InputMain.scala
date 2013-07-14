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
		val offsiteModel = SiteModel(gid)
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
		eb.addModel(offsiteModel, "offsiteModel")
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
		
		// userArm can transport from offsite
		eb.addRel(Rel("transporter-can", List("userArm", "offsite", "nil")))
		// A few other user-specified sites where the user can put plates on the robot
		eb.addRel(Rel("transporter-can", List("userArm", "hotel_245x1", "nil")))
		
		val labwareNamesOfInterest_l = new HashSet[String]
		labwareNamesOfInterest_l += "D-BSSE 96 Well PCR Plate"
		labwareNamesOfInterest_l += "D-BSSE 96 Well DWP"

		
		/*eb.addDevice(r1, pipetter, "pipetter")
		eb.addDevice(r1, shaker, "shaker")
		eb.addDevice(r1, thermocycler, "thermocycler")
		//eb.addDeviceModels(r1arm, List(m1, m2))
		eb.addDeviceModels(pipetter, List(m1, m2))
		//eb.addDeviceModels(sealer, List(m1))
		eb.addDeviceModels(shaker, List(m1, m2))
		eb.addDeviceModels(thermocycler, List(m1))
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
					// All models can be offsite
					eb.addStackable(offsiteModel, m)
					// The user arm can handle all models
					eb.addDeviceModel(userArm, m)
					//eb.addRel(Rel("transporter-can", List(eb.names(userArm), eb.names(m), "nil")))
				}
			}
			
			//
			// Create Sites
			//
			
			val siteEsToSiteModel_m = new HashMap[List[(Int, Int)], SiteModel]
			val siteIdToSite_m = new HashMap[(Int, Int), Site]
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
					eb.addSite(site, site.id)
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
					eb.addSite(site, carrierE.sName+site.id)
				}
			}
			
			// Create on-bench Sites for Plates
			for ((carrierE, grid_i) <- table.mapCarrierToGrid if !carriersSeen_l.contains(carrierE.id)) {
				for (site_i <- 0 until carrierE.nSites) {
					val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
					val siteId = (carrierE.id, site_i)
					val site = Site(siteId.toString)
					siteIdToSite_m(siteId) = site
					eb.addSite(site, f"bench_${grid_i}%03dx${site_i+1}")
				}
			}
			
			// TODO: Create on-bench Sites and SiteModels for Tubes
			// TODO: Let userArm handle tube models
			// TODO: Let userArm access all sites that the robot arms can't
			
			// Create SiteModels for for sites which hold Plates
			val siteIdToModels_m = new HashMap[(Int, Int), collection.mutable.Set[LabwareModel]] with MultiMap[(Int, Int), LabwareModel]
			
			{
				// First gather map of all relevant labware models that can be placed on each site 
				for (mE <- labwareModelEs if idToModel_m.contains(mE.sName)) {
					val m = idToModel_m(mE.sName)
					for (siteId <- mE.sites if siteIdToSite_m.contains(siteId)) {
						val site = siteIdToSite_m(siteId)
						siteIdToModels_m.addBinding(siteId, m)
					}
				}
				// Find all unique sets of labware models
				val unique = siteIdToModels_m.values.toSet
				val modelsToSiteModel_m = new HashMap[collection.mutable.Set[LabwareModel], SiteModel]
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
			
			// Create transporters
			val roma_m = new HashMap[Int, Transporter]()
			
			{
				// List of RoMa indexes
				val roma_li = carrier.mapCarrierToVectors.values.flatMap(_.map(_.iRoma)).toSet
				// Add transporter device for each RoMa
				for (roma_i <- roma_li) {
					val roma = Transporter(s"RoMa${roma_i + 1}")
					roma_m(roma_i) = roma
					eb.addDevice(r1, roma, s"r1_transporter${roma_i + 1}")
				}
			}
			
			// Find which sites the transporters can access
			for ((carrierE, vector_l) <- carrier.mapCarrierToVectors) {
				for (site_i <- 0 until carrierE.nSites) {
					val siteId = (carrierE.id, site_i)
					siteIdToSite_m.get(siteId).foreach { site =>
						for (vector <- vector_l) {
							val transporter = roma_m(vector.iRoma)
							eb.addRel(Rel("transporter-can", List(eb.names(transporter), eb.names(site), vector.sClass)))
						}
					}
				}
			}
			
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
			*/
			
			def addDevice(
				typeName: String,
				deviceName: String,
				carrierE: roboliq.evoware.parser.Carrier
			) {
				val carrierData = table.configFile
				
				// Add device
				val device = new Device { val id = carrierE.sName; val typeNames = List(typeName) }
				eb.addDevice(r1, device, deviceName)
		
				// Add device sites
				for (site_i <- 0 until carrierE.nSites) {
					val siteId = (carrierE.id, site_i)
					val site: Site = siteIdToSite_m(siteId)
					eb.addDeviceSite(device, site)
					siteIdToModels_m(siteId).foreach(m => eb.addDeviceModel(device, m))
				}
			}

			
			for ((carrierE, iGrid) <- table.mapCarrierToGrid) {
				carrierE.sName match {
					case "RoboSeal" =>
						addDevice("sealer", "sealer", carrierE)
					case "RoboPeel" =>
						addDevice("peeler", "peeler", carrierE)
					case _ =>
				}
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
