package roboliq.core

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.reflect.BeanProperty


abstract class Well {
	val id: String
	val idPlate: String
	val index: Int
	val iRow: Int
	val iCol: Int
	val indexName: String
}

class WellStatus {
	var bCheckVolume: Boolean = false
}

class PlateWell(
	val id: String,
	val idPlate: String,
	val index: Int,
	val iRow: Int,
	val iCol: Int,
	val indexName: String
) extends Part with Ordered[PlateWell] {
	def state(states: StateMap): WellState = states.findWellState(id) match {
		case Success(st) => st
		case _ => assert(false); null
	}
	def stateWriter(builder: StateBuilder): WellStateWriter = new WellStateWriter(id, builder)
	
	override def compare(that: PlateWell) = id.compare(that.id)
	override def toString = id
}
