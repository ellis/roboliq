package roboliq.core

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.LinkedHashMap

/**
 * A database for beans read in from YAML
 */
class BeanBase {
	private var m_lDevice: List[DeviceBean] = Nil
	private var m_lCmdHandler: List[CmdHandler] = Nil
	private val m_mapSystemProperties = new HashMap[String, Object]

	private val m_mapTipModel = new HashMap[String, TipModelBean]
	private val m_mapPlateModel = new HashMap[String, PlateModelBean]
	private val m_mapTubeModel = new HashMap[String, TubeModelBean]
	private val m_mapTip = new HashMap[String, TipBean]
	private val m_mapLocation = new LinkedHashMap[String, PlateLocationBean]
	private val m_mapTubeLocation = new LinkedHashMap[String, TubeLocationBean]

	private val m_mapSubstance = new HashMap[String, SubstanceBean]
	private val m_mapPlate = new HashMap[String, PlateBean]
	private val m_mapTube = new HashMap[String, PlateBean]
	private val m_lEvent = new ArrayBuffer[EventBean]
	
	/** List of devices */
	def lDevice = m_lDevice
	/** List of command handlers */
	def lCmdHandler = m_lCmdHandler
	/** Map of system properties.
	 * These values can be accessed by command handlers for the purpose of configuration.
	 */
	def mapSystemProperties: scala.collection.Map[String, Object] = m_mapSystemProperties

	/** Map of name to TipModel */
	def mapTipModel: scala.collection.Map[String, TipModelBean] = m_mapTipModel
	/** Map of name to PlateModel */
	def mapPlateModel: scala.collection.Map[String, PlateModelBean] = m_mapPlateModel
	/** Map of name to TubeModel */
	def mapTubeModel: scala.collection.Map[String, TubeModelBean] = m_mapTubeModel
	/** Map of name to Tip */
	def mapTip: scala.collection.Map[String, TipBean] = m_mapTip
	/** Map of name to Location */
	def mapLocation: scala.collection.Map[String, PlateLocationBean] = m_mapLocation
	/** Map of name to TubeLocation */
	def mapTubeLocation: scala.collection.Map[String, TubeLocationBean] = m_mapTubeLocation
	
	/** Map of name to Substance */
	def mapSubstance: scala.collection.Map[String, SubstanceBean] = m_mapSubstance
	/** Map of name to Plate */
	def mapPlate: scala.collection.Map[String, PlateBean] = m_mapPlate
	/** Map of name to Tube */
	def mapTube: scala.collection.Map[String, PlateBean] = m_mapTube
	/** List of events */
	def lEvent: scala.collection.Seq[EventBean] = m_lEvent
	
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
		// Add plate models
		if (bean.tubeModels != null) {
			bean.tubeModels.foreach(pair => pair._2._id = pair._1)
			m_mapTubeModel ++= bean.tubeModels
		}
		// Add tips
		if (bean.tips != null) {
			bean.tips.foreach(pair => pair._2._id = pair._1)
			m_mapTip ++= bean.tips
		}
		// Add plate locations
		if (bean.locations != null) {
			bean.locations.foreach(pair => pair._2._id = pair._1)
			m_mapLocation ++= bean.locations
		}
		// Add plate locations
		if (bean.tubeLocations != null) {
			bean.tubeLocations.foreach(pair => pair._2._id = pair._1)
			m_mapTubeLocation ++= bean.tubeLocations
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
		// Add tubes
		if (bean.tubes != null) {
			bean.tubes.foreach(pair => pair._2._id = pair._1)
			m_mapTube ++= bean.tubes
		}
		// Add events
		if (bean.events != null) {
			m_lEvent ++= bean.events
		}
	}
	
	//def findWellsBySubstance(idSubstance: String): List[String] = {
		
	//}
	
	/**
	 * Find a plate model with the given `id`.
	 * 
	 * @param id ID of plate model.
	 * @param property name of the bean property which should hold the ID (or null if not applicable).  This is used for error messages.
	 * @return a Success holding the PlateModelBean if the plate is found, otherwise an Error.
	 */
	def findPlateModel(id: String, property: String = null): Result[PlateModelBean] = {
		if (id == null) {
			if (property == null)
				Error("plate model ID not set")
			else
				Error("`"+property+"` must be set to plate model id")
		}
		else {
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
	
	/**
	 * Find a plate with ID `idPlate` and return a list of IDs for all its wells.
	 * 
	 * @param idPlate ID of plate.
	 * @return if plate found, a Success holding a list of IDs for all its wells, else Error.
	 */
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
