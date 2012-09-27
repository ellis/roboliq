package roboliq.core

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.reflect.BeanProperty


/** YAML JavaBean representation of [[roboliq.core.Plate]] or [[roboliq.core.Tube]]. */
class PlateBean extends Bean {
	/** ID of the plate's model */
	@BeanProperty var model: String = null
	/** Description of what the plate contains or is used for */
	@BeanProperty var description: String = null
	/** Plate barcode */
	@BeanProperty var barcode: String = null
	/** Location */
	@BeanProperty var location: String = null
}

/**
 * Represents a plate with wells (or a rack with tube slots).
 * 
 * @param id plate ID in the database.
 * @param model plate model.
 * @param locationPermanent_? opitonal location ID if this plate cannot be moved.
 */
class Plate(
	val id: String,
	val model: PlateModel,
	val locationPermanent_? : Option[String]
) extends Part with Ordered[Plate] {
	/** Number of rows. */
	def nRows: Int = model.nRows
	/** Number of columns. */
	def nCols: Int = model.nCols
	/** Number of wells. */
	def nWells: Int = nRows * nCols
	
	//override def createState(ob: ObjBase) = new PlateState(this, "")

	def state(states: StateMap): PlateState = states(this.id).asInstanceOf[PlateState]
	override def compare(that: Plate) = id.compare(that.id)
	override def toString = id
}

object Plate {
	/** Converts a plate bean to a plate. */
	def fromBean(ob: ObjBase)(bean: PlateBean): Result[Plate] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			idModel <- Result.mustBeSet(bean.model, "model")
			model <- ob.findPlateModel(idModel)
		} yield {
			val locationPermanent_? = if (bean.location != null) Some(bean.location) else None
			new Plate(id, model, locationPermanent_?)
		}
	}
}
