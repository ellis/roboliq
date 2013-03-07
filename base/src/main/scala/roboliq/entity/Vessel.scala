package roboliq.entity

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.reflect.BeanProperty


case class Vessel(
	id: String,
	tubeModel_? : Option[TubeModel]
)

/*
/**
 * Represents a vessel whose position on its plate is known.
 * A [[roboliq.core.PlateWell]] is an instance of `Well`,
 * whereas a [[roboliq.core.WellPosition]] must be created for a [[roboliq.core.Tube]] 
 * once its position on a rack has been determined.
 */
sealed trait Well extends Ordered[Well] {
	/** ID of plate in database. */
	val idPlate: String
	/** Index of well on plate. */
	val index: Int
	/** Index or well's row on plate (0-based). */
	val iRow: Int
	/** Index or well's column on plate (0-based). */
	val iCol: Int
	/** String representation of the well's plate location. */
	val indexName: String
	/** ID of well in database. */
	val id: String

	/** Get well's state. */
	def wellState(states: StateMap): Result[WellState] = states.findWellState(id)
	/** Get well's state writer. */
	def stateWriter(builder: StateBuilder): WellStateWriter = new WellStateWriter(id, builder)

	override def toString = id
	override def compare(that: Well) = id.compare(that.id)
}

/**
 * Represents a [[roboliq.core.Tube]] position on a particular rack.
 * 
 * @param id ID of vessel in database.
 * @param idPlate ID of plate in database.
 * @param index Index of well on plate.
 * @param iRow Index or well's row on plate (0-based).
 * @param iCol Index or well's column on plate (0-based).
 * @param indexName String representation of the well's plate location.
 */
case class WellPosition(
	val id: String,
	val idPlate: String,
	val index: Int,
	val iRow: Int,
	val iCol: Int,
	val indexName: String
) extends Well

object Well {
	/** Case a [[roboliq.core.PlateWell]] `o` to a [[roboliq.core.Well]]. */
	def apply(o: PlateWell): Well = {
		o
	}
	/** Get the [[roboliq.core.WellPosition]] object for a given tube `o`. */
	def forTube(o: TubeState, query: StateQuery): Result[Well] = {
		//println("WellPosition.forTube: "+o.obj.id)
		for { plate <- query.findPlate(o.idPlate) }
		yield {
			val index = o.row + o.col * plate.model.rows
			new WellPosition(o.obj.id, o.idPlate, index, o.row, o.col, "")
		}
	}
}

/**
 * Represents a well on a [[roboliq.core.Plate]].
 * 
 * @param idPlate ID of plate in database.
 * @param index Index of well on plate.
 * @param iRow Index or well's row on plate (0-based).
 * @param iCol Index or well's column on plate (0-based).
 * @param indexName String representation of the well's plate location.
 */
class PlateWell(
	id: String,
	val idPlate: String,
	val index: Int,
	val iRow: Int,
	val iCol: Int,
	val indexName: String
) extends Vessel(id) {
	@deprecated("use Well.wellState() instead", "0.1")
	def state(states: StateMap): WellState = states.findWellState(id) match {
		case Success(st) => st
		case _ => assert(false); null
	}
}

/**
 * Represents a tube.
 * 
 * @see [[roboliq.core.TubeModel]]
 * 
 * @param model Model of tube.
 */
class Tube(
	id: String,
	val model: TubeModel
) extends Vessel(id) {
	@deprecated("use Well.wellState() instead", "0.1")
	def state(states: StateMap): WellState = states.findWellState(id) match {
		case Success(st) => st
		case _ => assert(false); null
	}
	
	//override def compare(that: Well) = id.compare(that.id)
	override def toString = id
}

object Tube {
	/** Convert [[roboliq.core.PlateBean]] to [[roboliq.core.Tube]]. */
	def fromBean(ob: ObjBase)(bean: PlateBean): Result[Tube] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			idModel <- Result.mustBeSet(bean.model, "model")
			model <- ob.findTubeModel(idModel)
		} yield {
			new Tube(id, model)
		}
	}
}
*/