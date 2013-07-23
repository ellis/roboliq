package roboliq.input

import spray.json._
import spray.json.DefaultJsonProtocol._
import roboliq.entities.Entity
import scala.collection.mutable.ArrayBuffer
import roboliq.entities._
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import roboliq.utils.FileUtils

/*class PlateBean {
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
)*/

object InputMain extends App {
	val protocol = new Protocol
	
	val carrier = roboliq.evoware.parser.EvowareCarrierData.loadFile("./testdata/bsse-robot1/config/carrier.cfg").getOrElse(null)
	val table = roboliq.evoware.parser.EvowareTableData.loadFile(carrier, "./testdata/bsse-robot1/config/table-01.esc").getOrElse(null)
	protocol.loadConfig()
	protocol.loadEvoware("r1", carrier, table)
	protocol.loadJson("""
{
"plates": [
	{ "name": "plate1", "model": "Thermocycler Plate", "location": "offsite"}
],
"protocol": [
	{ "command": "shake", "object": "plate1" },
	{ "command": "seal", "object": "plate1" }
]
}""".asJson.asJsObject
	)

	/*
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
	*/
	
	protocol.saveProblem("pb")
}
