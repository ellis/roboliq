package roboliq.core

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

class BeanBase {
	private val m_mapPlateModel = new HashMap[String, PlateModelBean]
	private val m_mapPlate = new HashMap[String, PlateBean]
	private val m_mapSubstance = new HashMap[String, SubstanceItem]
	private val m_mapHistory = new HashMap[String, List[HistoryItem]]
	
	def mapPlateModel: scala.collection.Map[String, PlateModelBean] = m_mapPlateModel
	def mapPlate: scala.collection.Map[String, PlateBean] = m_mapPlate
	def mapSubstance: scala.collection.Map[String, SubstanceItem] = m_mapSubstance
	def mapHistory: scala.collection.Map[String, List[HistoryItem]] = m_mapHistory
	
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
		// Add history
		for ((id, history) <- bean.history) {
			m_mapHistory(id) = history.toList
		}
	}
	
	//def findWellsBySubstance(idSubstance: String): List[String] = {
		
	//}
}
