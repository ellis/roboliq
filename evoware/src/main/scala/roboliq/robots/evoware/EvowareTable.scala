package roboliq.robots.evoware

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap


abstract class EvowareTable(configFile: EvowareConfigFile, sFilename: String) {
	val tableFile = EvowareTableParser.parseFile(configFile, sFilename)
	//tableFile.print()

	private val m_mapLabelToSite = new HashMap[String, CarrierSite]
	private val m_mapLabelToLabware = new HashMap[String, LabwareObject]
	
	def mapLabelToSite = m_mapLabelToSite.toMap
	def mapLabelToLabware = m_mapLabelToLabware.toMap
	
	def labelSite(sLabel: String, sCarrierName: String, iSite: Int): CarrierSite = {
		val carrier = configFile.mapNameToCarrier(sCarrierName)
		val site = CarrierSite(carrier, iSite)
		m_mapLabelToSite(sLabel) = site
		site
	}
	
	def labelSites(lsLabel: List[String], sCarrierName: String): List[CarrierSite] = {
		lsLabel.zipWithIndex.map(pair => labelSite(pair._1, sCarrierName, pair._2))
	}
	
	def labelLabware(sLabel: String, sCarrierName: String, iSite: Int): LabwareObject = {
		val carrier = configFile.mapNameToCarrier(sCarrierName)
		val site = CarrierSite(carrier, iSite)
		val labware_? = tableFile.lLabwareObject.find(_.site == site) match {
			case Some(o) => Some(o)
			case None =>
				tableFile.lExternalLabwareObject.find(_.site == site)
		}
		val labware = labware_?.get
		m_mapLabelToLabware(sLabel) = labware
		labware
	}
	
	def labelLabwares(lsLabel: List[String], sCarrierName: String): List[LabwareObject] = {
		lsLabel.zipWithIndex.map(pair => labelLabware(pair._1, sCarrierName, pair._2))
	}
	
/*
	private val m_sites = new ArrayBuffer[SiteObj]
	private val m_mapSites = new HashMap[String, SiteObj]
	
	def mapSites = m_mapSites.toMap
	def sites = m_sites.toIterable
	
	private def addSite(site: SiteObj) {
		m_sites += site
		// TODO: produce an error message here instead, and propagate it back up
		assert(!m_mapSites.contains(site.sName))
		m_mapSites(site.sName) = site //new Site(site.carrier.iGrid, site.iSite)
	}
	
	protected def createSite(carrier: CarrierObj, iSite: Int, s1: String, liRoma: Seq[Int]): SiteObj = {
		val o = new SiteObj(s1, carrier, iSite, liRoma)
		addSite(o)
		o
	}
	protected def createSites(carrier: CarrierObj, s1: String, liRoma: Seq[Int]): SiteObj = {
		val o = new SiteObj(s1, carrier, 0, liRoma)
		addSite(o)
		o
	}
	protected def createSites(carrier: CarrierObj, s1: String, s2: String, liRoma: Seq[Int]): Tuple2[SiteObj, SiteObj] = {
		val o = (new SiteObj(s1, carrier, 0, liRoma), new SiteObj(s2, carrier, 1, liRoma))
		addSite(o._1)
		addSite(o._2)
		o
	}
	protected def createSites(carrier: CarrierObj, s1: String, s2: String, s3: String, liRoma: Seq[Int]): Tuple3[SiteObj, SiteObj, SiteObj] = {
		val o = (new SiteObj(s1, carrier, 0, liRoma), new SiteObj(s2, carrier, 1, liRoma), new SiteObj(s3, carrier, 2, liRoma))
		addSite(o._1)
		addSite(o._2)
		addSite(o._3)
		o
	}
*/
}
