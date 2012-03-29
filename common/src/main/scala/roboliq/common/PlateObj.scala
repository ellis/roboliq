package roboliq.common

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.reflect.BeanProperty


/** Represents a plate in YAML */
class PlateBean extends Bean {
	/** ID of the plate's model */
	@BeanProperty var model: String = null
	/** Description of what the plate contains or is used for */
	@BeanProperty var description: String = null
	/** Plate barcode */
	@BeanProperty var barcode: String = null
}

class PlateObj extends Obj {
	thisObj =>
	type Config = Plate
	type State = PlateStateL2
	
	var sLabel_? : Option[String] = None
	var model_? : Option[PlateModel] = None
	var dim_? : Option[PlateSetupDimensionL4] = None
	var location_? : Option[String] = None
	
	def setDimension(rows: Int, cols: Int) {
		assert(dim_?.isEmpty)
		
		val nWells = rows * cols
		val wells = (0 until nWells).map(i => {
			val well = new Well
			well.index_? = Some(i)
			well.holder_? = Some(this)
			well
		})
		val dim = new PlateSetupDimensionL4(rows, cols, wells.toSeq)
		dim_? = Some(dim)
	}

	override def getLabel(kb: KnowledgeBase): String = {
		sLabel_? match {
			case Some(s) => s
			case None => toString
		}
	}

	def createConfigAndState0(): Result[Tuple2[Config, State]] = {
		val setup = this
		val errors = new ArrayBuffer[String]

		if (setup.sLabel_?.isEmpty)
			errors += "label not set"
		if (setup.dim_?.isEmpty)
			errors += "dimension not set"
		if (setup.location_?.isEmpty)
			errors += "location not set"
		if (!errors.isEmpty)
			return Error(errors)

		val dim = setup.dim_?.get
		
		val conf = new Plate(
			obj = this,
			sLabel = setup.sLabel_?.get,
			model_? = setup.model_?,
			nRows = dim.nRows,
			nCols = dim.nCols,
			nWells = dim.nRows * dim.nCols,
			wells = dim.wells)
		val state = new PlateStateL2(
			conf = conf,
			location = setup.location_?.get)

		Success(conf, state)
	}

	class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]
		
		def location = state.location
		def location_=(location: String) { map(thisObj) = state.copy(location = location) }
	}
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
	
	override def toString = sLabel_?.getOrElse(super.toString)
}

class PlateSetupDimensionL4(
	val nRows: Int,
	val nCols: Int,
	val wells: Seq[Well]
)

class Plate(
	val obj: PlateObj,
	val sLabel: String,
	val model_? : Option[PlateModel],
	val nRows: Int,
	val nCols: Int,
	val nWells: Int,
	val wells: Seq[Well]
) extends ObjConfig with Ordered[Plate] {
	def state(states: StateMap) = obj.state(states)
	override def compare(that: Plate) = sLabel.compare(that.sLabel)
	override def toString = sLabel
}

case class PlateStateL2(
	val conf: Plate,
	val location: String
) extends ObjState


class PlateProxy(kb: KnowledgeBase, obj: PlateObj) {
	def label = obj.sLabel_?.get
	def label_=(s: String) { obj.sLabel_? = Some(s) }
	
	def setDimension(rows: Int, cols: Int) {
		obj.setDimension(rows, cols)
		obj.dim_?.get.wells.foreach(kb.addWell)
	}
	
	def location = obj.location_?.get
	def location_=(s: String) { obj.location_? = Some(s) }
	
	def wells = obj.dim_?.get.wells
}
