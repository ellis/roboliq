package roboliq.core

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty


/**
 * YAML JavaBean for [[roboliq.core.PlateLocation]]
 */
class PlateLocationBean extends Bean {
	/** List of the names of plate models which can be at this location. */
	@BeanProperty var plateModels: java.util.List[String] = null
	/** Whether this location is cooled. */
	@BeanProperty var cooled: java.lang.Boolean = null
}

/**
 * YAML JavaBean for [[roboliq.core.TubeLocation]]
 */
class TubeLocationBean extends Bean {
	/** List of the names of tube models which can be at this location. */
	@BeanProperty var tubeModels: java.util.List[String] = null
	/** Name of the rack model at this location which will hold the tubes */ 
	@BeanProperty var rackModel: String = null
	/** Whether this location is cooled. */
	@BeanProperty var cooled: java.lang.Boolean = null
}

/**
 * Represents a bench location.
 */
sealed abstract class Location {
	val id: String
}

/**
 * Represents a bench location to hold plates.
 * 
 * @param id unique ID for this location
 * @param plateModels list of plate models which can be at this location.
 * @param cooled whether this location is cooled.
 */
case class PlateLocation(
	val id: String,
	val plateModels: List[PlateModel],
	val cooled: Boolean
) extends Location

object PlateLocation {
	/**
	 * Convert YAML JavaBean [[roboliq.core.PlateLocationBean]] to a PlateLocation.
	 */
	def fromBean(ob: ObjBase)(bean: PlateLocationBean): Result[PlateLocation] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			plateModels <- Result.mustBeSet(bean.plateModels, "model")
			lModel <- Result.mapOver(plateModels.toList)(ob.findPlateModel)
		} yield {
			val cooled: Boolean = if (bean.cooled != null) bean.cooled else false
			new PlateLocation(id, lModel, cooled)
		}
	}
}

/**
 * Represents a bench location to hold tubes.
 * 
 * @param id unique ID for this location
 * @param rackModel rack model at this location which will hold the tubes 
 * @param tubeModels list of the tube models which can be at this location.
 * @param cooled whether this location is cooled.
 */
case class TubeLocation(
	val id: String,
	val rackModel: PlateModel,
	val tubeModels: List[TubeModel],
	val cooled: Boolean
) extends Location

object TubeLocation {
	/**
	 * Convert YAML JavaBean [[roboliq.core.TubeLocationBean]] to a TubeLocation.
	 */
	def fromBean(ob: ObjBase)(bean: TubeLocationBean): Result[TubeLocation] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			_ <- Result.mustBeSet(bean.rackModel, "rackModel")
			tubeModels <- Result.mustBeSet(bean.tubeModels, "model")
			lTubeModel <- Result.mapOver(tubeModels.toList)(ob.findTubeModel)
			rackModel <- ob.findPlateModel(bean.rackModel)
		} yield {
			val cooled: Boolean = if (bean.cooled != null) bean.cooled else false
			new TubeLocation(id, rackModel, lTubeModel, cooled)
		}
	}
}