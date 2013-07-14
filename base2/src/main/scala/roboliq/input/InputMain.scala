package roboliq.input

import spray.json.JsObject
import spray.json.JsString
import roboliq.entities.Entity
import scala.collection.mutable.ArrayBuffer
import roboliq.entities._
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap

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
		/*eb.addDevice(user, userArm, "userArm")
		eb.addDevice(r1, pipetter, "pipetter")
		eb.addDevice(r1, shaker, "shaker")
		eb.addDevice(r1, thermocycler, "thermocycler")
		eb.addDeviceModels(userArm, List(m1, m2))
		eb.addDeviceModels(r1arm, List(m1, m2))
		eb.addDeviceModels(pipetter, List(m1, m2))
		eb.addDeviceModels(sealer, List(m1))
		eb.addDeviceModels(shaker, List(m1, m2))
		eb.addDeviceModels(thermocycler, List(m1))
		eb.addDeviceSites(userArm, List(offsite, s1))
		eb.addDeviceSites(r1arm, List(s1, s2, thermocyclerSite))
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
			val labwareModels = carrier.models.collect({case m: roboliq.evoware.parser.LabwareModel => m})
			val idToModel_m = new HashMap[String, LabwareModel]
			eb.addAgent(r1, "r1")
			
			// List of RoMa indexes
			val roma_li = carrier.mapCarrierToVectors.values.flatMap(_.map(_.iRoma)).toSet
			val roma_m = new HashMap[Int, Transporter]
			// Add transporter device for each RoMa
			for (roma_i <- roma_li) {
				val roma = Transporter(s"RoMa${roma_i + 1}")
				roma_m(roma_i) = roma
				eb.addDevice(r1, roma, s"r1arm${roma_i + 1}")
			}

			for ((carrierE, iGrid) <- table.mapCarrierToGrid) {
				carrierE.sName match {
					case "RoboSeal" =>
						val sealer = Sealer(carrierE.sName)
						eb.addDevice(r1, sealer, "sealer")
						for (site_i <- 0 until carrierE.nSites) {
							// Create site and it's model (each site requires its own model)
							val sealerSiteModel = SiteModel(gid)
							val sealerSite = Site(s"site_${iGrid}x${site_i+1}")
							eb.addModel(sealerSiteModel, s"sealerSiteModel${site_i+1}")
							eb.addSite(sealerSite, s"sealerSite${site_i+1}")
							eb.setModel(sealerSite, sealerSiteModel)
							eb.addDeviceSite(sealer, sealerSite)
							// List transporters which can access this site
							// NOTE: SHOULD USE TRANSPORTER SPEC (== Vector)
							for (vector <- carrier.mapCarrierToVectors.getOrElse(carrierE, Nil)) {
								val roma = roma_m(vector.iRoma)
								eb.addDeviceSite(roma, sealerSite)
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
								eb.addDeviceModel(sealer, model)
								eb.addStackable(sealerSiteModel, model)
							}
						}
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
