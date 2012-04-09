package roboliq.core

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.reflect.BeanProperty


sealed abstract class Well(val id: String) extends Part with Ordered[Well] {
	def wellState(states: StateMap): Result[WellState] = states.findWellState(id)
	def stateWriter(builder: StateBuilder): WellStateWriter = new WellStateWriter(id, builder)

	override def compare(that: Well) = id.compare(that.id)
	override def toString = id
}

class WellPosition(
	val idPlate: String,
	val index: Int,
	val iRow: Int,
	val iCol: Int,
	val indexName: String
)

object WellPosition {
	def apply(o: PlateWell): WellPosition = {
		new WellPosition(o.idPlate, o.index, o.iRow, o.iCol, o.indexName)
	}
	def forTube(o: TubeState, query: StateQuery): Result[WellPosition] = {
		for { plate <- query.findPlate(o.idPlate) }
		yield {
			val index = o.row + o.col * plate.model.nRows
			new WellPosition(o.obj.id, index, o.row, o.col, "")
		}
	}
}

class WellStatus {
	var bCheckVolume: Boolean = false
}

class PlateWell(
	id: String,
	val idPlate: String,
	val index: Int,
	val iRow: Int,
	val iCol: Int,
	val indexName: String
) extends Well(id) {
	def state(states: StateMap): WellState = states.findWellState(id) match {
		case Success(st) => st
		case _ => assert(false); null
	}
}

class Tube(
	id: String,
	val model: TubeModel
) extends Well(id) {
	def state(states: StateMap): WellState = states.findWellState(id) match {
		case Success(st) => st
		case _ => assert(false); null
	}
	
	override def compare(that: Well) = id.compare(that.id)
	override def toString = id
}

object Tube {
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