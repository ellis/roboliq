package roboliq.yaml

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

class YamlDatabase {
	private val m_mapPlateModel = new HashMap[String, PlateModelBean]
	private val m_mapPlate = new HashMap[String, PlateBean]
	private val m_mapSubstance = new HashMap[String, SubstanceItem]
	
	/** Add data in @param bean to this database */
	def addBean(bean: RoboliqYamlBean) {
		// Add plate models
		bean.plateModels.foreach(pair => pair._2._id = pair._1)
		m_mapPlateModel ++= bean.plateModels
		// Add plates
		bean.plates.foreach(pair => pair._2._id = pair._1)
		m_mapPlate ++= bean.plates
		// Add substances
		bean.substances.foreach(pair => pair._2._id = pair._1)
		m_mapSubstance ++= bean.substances
	}
	
	//def findWellsBySubstance(idSubstance: String): List[String] = {
		
	//}
}
/*
trait ItemDatabase {
	val lPlate: List[Plate]
	def lookupItem(pair: Tuple2[String, String]): Option[Item]
	def lookupItem(route: Route): Option[Item] = lookupItem(route.toPair)
	def lookup[A](pair: Tuple2[String, String]): Option[A]
	def findWellsByLiquid(sLiquidKey: String): List[Well]
	def findWellsByPlateKey(sPlateKey: String): List[Well]
	def findPlateByPurpose(sPurpose: String): List[Plate]
}

*/