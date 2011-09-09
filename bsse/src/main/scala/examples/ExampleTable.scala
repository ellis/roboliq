package examples

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common
import roboliq.common._
import evoware._


trait ExampleTable {
	//val table = new {
		//object BUF12 extends roboliq.protocol.PlateFixedPlate(8, 1, "BUF12")
	//}
	object Location {
		val P4 = "P4"
		val P5 = "P5"
		val P6 = "P6"
	}
	object PlateModel {
		val Standard96 = new common.PlateModel("D-BSSE 96 Deep Well Plate", 8, 12, 20000)
	}
}

trait EvowareTable {
	/*private val m_carrierModels = new ArrayBuffer[CarrierModel]
	
	protected def CarrierModel(sName: String, nSites: Int, bCooled: Boolean): CarrierModel = {
		val o = new CarrierModel(sName, nSites, bCooled)
		m_carrierModels += o
		o
	}*/
	private val m_sites = new ArrayBuffer[SiteObj]
	private val m_mapSites = new HashMap[String, Site]
	
	def mapSites = m_mapSites.toMap
	
	private def addSite(site: SiteObj) {
		m_sites += site
		// TODO: produce an error message here instead, and propagate it back up
		assert(!m_mapSites.contains(site.sName))
		m_mapSites(site.sName) = new Site(site.carrier.iGrid, site.iSite)
	}
	
	protected def createSites(carrier: CarrierObj, s1: String): SiteObj = {
		val o = new SiteObj(s1, carrier, 0)
		addSite(o)
		o
	}
	protected def createSites(carrier: CarrierObj, s1: String, s2: String): Tuple2[SiteObj, SiteObj] = {
		val o = (new SiteObj(s1, carrier, 0), new SiteObj(s2, carrier, 1))
		addSite(o._1)
		addSite(o._2)
		o
	}
	protected def createSites(carrier: CarrierObj, s1: String, s2: String, s3: String): Tuple3[SiteObj, SiteObj, SiteObj] = {
		val o = (new SiteObj(s1, carrier, 0), new SiteObj(s2, carrier, 1), new SiteObj(s3, carrier, 2))
		addSite(o._1)
		addSite(o._2)
		addSite(o._3)
		o
	}
}

trait ExampleTable2 extends EvowareTable {
	import evoware.PlateModel
	
	object CarrierModels {
		val wash = new CarrierModel("wash", 3, false)
		val decon = new CarrierModel("decon", 3, false)
		val reagents = new CarrierModel("reagent columns", 2, true)
		val ethanol = new CarrierModel("ethanol", 1, true)
		val shaker = new CarrierModel("shaker", 1, false)
		val holder = new CarrierModel("holder", 2, true)
		val epindorfs = new CarrierModel("epindorfs", 1, true)
		val cooled = new CarrierModel("cooled", 2, true)
		val uncooled = new CarrierModel("uncooled", 3, false)
		val filters = new CarrierModel("filters", 2, false)
	}
	object Carriers {
		val wash1 = new CarrierObj("wash1", CarrierModels.wash, 1)
		val wash2 = new CarrierObj("wash2", CarrierModels.wash, 2)
		val decon = new CarrierObj("decon", CarrierModels.decon, 3)
		val reagents = new CarrierObj("reagents", CarrierModels.reagents, 4)
		val ethanol = new CarrierObj("ethanol", CarrierModels.ethanol, 7)
		val shaker = new CarrierObj("shaker", CarrierModels.shaker, 9)
		val holder = new CarrierObj("holder", CarrierModels.holder, 10)
		val epindorfs = new CarrierObj("epindorfs", CarrierModels.epindorfs, 16)
		val cooled = new CarrierObj("cooled", CarrierModels.cooled, 17)
		val uncooled = new CarrierObj("uncooled", CarrierModels.uncooled, 24)
		val filters = new CarrierObj("filters", CarrierModels.filters, 33)
	}
	object Sites {
		val (wash1a, wash1b, wash1c) = createSites(Carriers.wash1, "wash1a", "wash1b", "wash1c")
		val (wash2a, wash2b, wash2c) = createSites(Carriers.wash2, "wash2a", "wash2b", "wash2c")
		val (decon1, decon2, decon3) = createSites(Carriers.decon, "decon1", "decon2", "decon3")
		val (reagents15, reagents50) = createSites(Carriers.reagents, "reagents15", "reagents50")
		val ethanol = createSites(Carriers.ethanol, "ethanol")
		val shaker = createSites(Carriers.shaker, "shaker")
		val (holder, cover) = createSites(Carriers.holder, "holder", "cover")
		val epindorfs = createSites(Carriers.epindorfs, "epindorfs")
		val (cooled1, cooled2) = createSites(Carriers.cooled, "cooled1", "cooled2")
		val (uncooled1, uncooled2, uncooled3) = createSites(Carriers.uncooled, "uncooled1", "uncooled2", "uncooled3")
		val (filter1, filter2) = createSites(Carriers.filters, "filter1", "filter2")
	}
	object LabwareModels {
		val washA = new TroughModel("Wash Station Cleaner shallow", 8, 1)
		val washB = new TroughModel("Wash Station Waste", 8, 1)
		val washC = new TroughModel("Wash Station Cleaner deep", 8, 1)
		val decon = new TroughModel("Trough 100ml", 8, 1)
		val reagents15 = new PlateModel("Reagent Cooled 8*15ml", 8, 1, 15000)
		val reagents50 = new PlateModel("Reagent Cooled 8*50ml", 8, 1, 50000)
		val ethanol = new TroughModel("Trough 1000ml", 8, 1)
		val platePcr = new PlateModel("D-BSSE 96 Well PCR Plate", 8, 12, 500) // FIXME: nVolume? 
		val epindorfs = new PlateModel("...", 4, 5, 1500) // FIXME: nVolume?
		val test4x3 = new PlateModel("test 4x3 Plate", 4, 3, 500) // FIXME: nVolume? 
	}
	object Labwares {
		val wash1a = new TroughObj("wash1a", LabwareModels.washA, Sites.wash1a)
		val wash1b = new TroughObj("wash1b", LabwareModels.washB, Sites.wash1b)
		val wash1c = new TroughObj("wash1c", LabwareModels.washC, Sites.wash1c)
		val wash2a = new TroughObj("wash2a", LabwareModels.washA, Sites.wash2a)
		val wash2b = new TroughObj("wash2b", LabwareModels.washB, Sites.wash2b)
		val wash2c = new TroughObj("wash2c", LabwareModels.washC, Sites.wash2c)
		val decon1 = new TroughObj("decon1", LabwareModels.decon, Sites.decon1)
		val decon2 = new TroughObj("decon2", LabwareModels.decon, Sites.decon2)
		val decon3 = new TroughObj("decon3", LabwareModels.decon, Sites.decon3)
		val reagents15 = new PlateObj("reagents15", LabwareModels.reagents15, Sites.reagents15)
		val reagents50 = new PlateObj("reagents50", LabwareModels.reagents50, Sites.reagents50)
		val ethanol = new TroughObj("ethanol", LabwareModels.ethanol, Sites.ethanol)
		val epindorfs = new PlateObj("epindorfs", LabwareModels.epindorfs, Sites.epindorfs)
	}
}
