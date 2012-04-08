package roboliq.core

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

class BeanBase {
	private var m_lDevice: List[DeviceBean] = Nil
	private var m_lCmdHandler: List[CmdHandler] = Nil
	private val m_mapSystemProperties = new HashMap[String, Object]

	private val m_mapTipModel = new HashMap[String, TipModelBean]
	private val m_mapPlateModel = new HashMap[String, PlateModelBean]
	private val m_mapTip = new HashMap[String, TipBean]
	private val m_mapLocation = new HashMap[String, LocationBean]

	private val m_mapSubstance = new HashMap[String, SubstanceBean]
	private val m_mapPlate = new HashMap[String, PlateBean]
	private val m_mapEvents = new HashMap[String, List[EventBean]]
	
	def mapSystemProperties: scala.collection.Map[String, Object] = m_mapSystemProperties
	def mapTipModel: scala.collection.Map[String, TipModelBean] = m_mapTipModel
	def mapPlateModel: scala.collection.Map[String, PlateModelBean] = m_mapPlateModel
	def mapTip: scala.collection.Map[String, TipBean] = m_mapTip
	def mapLocation: scala.collection.Map[String, LocationBean] = m_mapLocation
	def mapSubstance: scala.collection.Map[String, SubstanceBean] = m_mapSubstance
	def mapPlate: scala.collection.Map[String, PlateBean] = m_mapPlate
	def mapEvents: scala.collection.Map[String, List[EventBean]] = m_mapEvents
	def lDevice = m_lDevice
	def lCmdHandler = m_lCmdHandler
	
	/** Add data in @param bean to this database */
	def addBean(bean: RoboliqYamlBean) {
		// Add devices
		if (bean.devices != null) {
			m_lDevice = m_lDevice ++ bean.devices.toList
		}
		// Add command handlers in reverse order so that the last once defined has priority
		if (bean.commandHandlers != null) {
			m_lCmdHandler = bean.commandHandlers.toList.reverse ++ m_lCmdHandler
		}
		// Add system properties
		if (bean.systemProperties != null) {
			m_mapSystemProperties ++= bean.systemProperties
		}
		
		// Add tip models
		if (bean.tipModels != null) {
			bean.tipModels.foreach(pair => pair._2._id = pair._1)
			m_mapTipModel ++= bean.tipModels
		}
		// Add plate models
		if (bean.plateModels != null) {
			bean.plateModels.foreach(pair => pair._2._id = pair._1)
			m_mapPlateModel ++= bean.plateModels
		}
		// Add tips
		if (bean.tips != null) {
			bean.tips.foreach(pair => pair._2._id = pair._1)
			m_mapTip ++= bean.tips
		}
		// Add locations
		if (bean.locations != null) {
			bean.locations.foreach(pair => pair._2._id = pair._1)
			m_mapLocation ++= bean.locations
		}
		
		// Add substances
		if (bean.substances != null) {
			bean.substances.foreach(pair => pair._2._id = pair._1)
			m_mapSubstance ++= bean.substances
		}
		// Add plates
		if (bean.plates != null) {
			bean.plates.foreach(pair => pair._2._id = pair._1)
			m_mapPlate ++= bean.plates
		}
		// Add events
		if (bean.events != null) {
			for ((id, events) <- bean.events) {
				events.foreach(_.obj = id)
				m_mapEvents(id) = events.toList
			}
		}
	}
	
	//def findWellsBySubstance(idSubstance: String): List[String] = {
		
	//}
	
	def findPlateModel(id: String, property: String = null): Result[PlateModelBean] = {
		if (id == null) {
			if (property == null)
				Error("plate model ID not set")
			else
				Error("`"+property+"` must be set to plate model id")
		}
		else {
			if (!mapPlateModel.contains(id)) {
			}
			mapPlateModel.get(id) match {
				case Some(obj) => Success(obj)
				case None =>
					val obj = new PlateModelBean
					obj._id = id
					m_mapPlateModel(id) = obj
					Success(obj)
			}
		}
	}
	
	def findWellIdsByPlate(idPlate: String): Result[List[String]] = {
		for {
			plateModel <- findPlateModel(idPlate)
			rows <- Result.mustBeSet(plateModel.rows, "rows")
			cols <- Result.mustBeSet(plateModel.cols, "cols")
		} yield {
			for {
				col <- (1 to cols).toList
				row <- 1 to rows
			} yield {
				(row + 'A').asInstanceOf[Char].toString + ("%02d".format(col))
			}
		}
	}
}
