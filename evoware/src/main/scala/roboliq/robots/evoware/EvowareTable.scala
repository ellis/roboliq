package roboliq.robots.evoware

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap


trait EvowareTable {
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
