package roboliq.core

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.reflect.BeanProperty


abstract class Well extends Part with Ordered[Well] {
	val id: String
	val idPlate: String
	val index: Int
	val iRow: Int
	val iCol: Int
	val indexName: String
	
	def state(states: StateMap): WellState = states(this.id).asInstanceOf[WellState]
	def stateWriter(builder: StateBuilder): WellStateWriter = new WellStateWriter(this, builder)
	
	override def compare(that: Well) = id.compare(that.id)
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
) extends Well {
	/*override def createState(ob: ObjBase) = new PlateWellState(
		this, Liquid.empty, LiquidVolume.empty, false
	)*/
}

class Tube(
	val id: String
) extends Well {
	val idPlate = id
	val index = 0
	val iRow = 0
	val iCol = 0
	val indexName = ""
}