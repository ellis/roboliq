package roboliq.robots.evoware

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap


abstract class EvowareTable(configFile: EvowareCarrierData, sFilename: String) {
	val tableFile = EvowareTableParser.parseFile(configFile, sFilename)
	//tableFile.print()

	// A site can be given multiple labels
	private val m_mapLabelToSite = new HashMap[String, CarrierSite]
	
	def mapLabelToSite = m_mapLabelToSite.toMap
	
	def labelSite(sLabel: String, sCarrierName: String, iSite: Int): CarrierSite = {
		// FIXME: for debug only
		if (!configFile.mapNameToCarrier.contains(sCarrierName)) {
			println("labelSite missing")
			configFile.mapNameToCarrier.keys.foreach(println)
		}
		// ENDFIX
		val carrier = configFile.mapNameToCarrier(sCarrierName)
		val site = CarrierSite(carrier, iSite)
		m_mapLabelToSite(sLabel) = site
		site
	}
	
	def labelSites(lsLabel: List[String], sCarrierName: String): List[CarrierSite] = {
		lsLabel.zipWithIndex.map(pair => labelSite(pair._1, sCarrierName, pair._2))
	}
}
