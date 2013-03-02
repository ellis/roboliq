package roboliq.core

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty


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
