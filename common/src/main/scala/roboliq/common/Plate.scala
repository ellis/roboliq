package roboliq.common

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap


class PlateModel(val id: String, val nRows: Int, val nCols: Int, val nWellVolume: Double)

class Plate extends Obj {
	thisObj =>
	type Setup = PlateSetup
	type Config = PlateConfigL2
	type State = PlateStateL2
	
	val setup = new Setup(this)
	def createSetup() = setup
	
	def createConfigAndState0(setup: Setup): Result[Tuple2[Config, State]] = {
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
		
		val conf = new PlateConfigL2(
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
}

class PlateSetup(val obj: Plate) extends ObjSetup {
	var sLabel_? : Option[String] = None
	var model_? : Option[PlateModel] = None
	var dim_? : Option[PlateSetupDimensionL4] = None
	var location_? : Option[String] = None
	
	def setDimension(rows: Int, cols: Int) {
		assert(dim_?.isEmpty)
		
		val nWells = rows * cols
		val wells = (0 until nWells).map(i => {
			val well = new Well
			val wellSetup = well.setup
			wellSetup.index_? = Some(i)
			wellSetup.holder_? = Some(obj)
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
}

class PlateSetupDimensionL4(
	val nRows: Int,
	val nCols: Int,
	val wells: Seq[Well]
)

class PlateConfigL2(
	val obj: Plate,
	val sLabel: String,
	val model_? : Option[PlateModel],
	val nRows: Int,
	val nCols: Int,
	val nWells: Int,
	val wells: Seq[Well]
) extends ObjConfig with Ordered[PlateConfigL2] {
	def state(states: StateMap) = obj.state(states)
	override def compare(that: PlateConfigL2) = sLabel.compare(that.sLabel)
	override def toString = sLabel
}

case class PlateStateL2(
	val conf: PlateConfigL2,
	val location: String
) extends ObjState


class PlateProxy(kb: KnowledgeBase, obj: Plate) {
	val setup = kb.getPlateSetup(obj)
	
	def label = setup.sLabel_?.get
	def label_=(s: String) { setup.sLabel_? = Some(s) }
	
	def setDimension(rows: Int, cols: Int) {
		obj.setup.setDimension(rows, cols)
		obj.setup.dim_?.get.wells.foreach(kb.addWell)
	}
	
	def location = setup.location_?.get
	def location_=(s: String) { setup.location_? = Some(s) }
	
	def wells = setup.dim_?.get.wells
}
