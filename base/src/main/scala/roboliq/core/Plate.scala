package roboliq.core

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
	/** Location */
	@BeanProperty var location: String = null
}

class Plate(
	val id: String,
	val model: PlateModel,
	val locationPermanent_? : Option[String]
) extends Part with Ordered[Plate] {
	def nRows: Int = model.nRows
	def nCols: Int = model.nCols
	def nWells: Int = nRows * nCols
	
	//override def createState(ob: ObjBase) = new PlateState(this, "")

	def state(states: StateMap): PlateState = states(this.id).asInstanceOf[PlateState]
	override def compare(that: Plate) = id.compare(that.id)
	override def toString = id
}

object Plate {
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

case class PlateState(
	val conf: Plate,
	val location: String
)

class PlateStateWriter(o: Plate, builder: StateBuilder) {
	def state = builder.map(o.id).asInstanceOf[PlateState]
	
	private def set(state1: PlateState) { builder.map(o.id) = state1 }
	
	def location = state.location
	def location_=(location: String) { set(state.copy(location = location)) }
}

/*
class PlateObj extends Obj {
	thisObj =>
	type Config = Plate
	type State = PlateStateL2
	
	var sLabel_? : Option[String] = None
	var model_? : Option[PlateModel] = None
	var dim_? : Option[PlateSetupDimensionL4] = None
	var location_? : Option[String] = None
	
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
		if (setup.location_?.isEmpty)
			errors += "location not set"
		if (setup.model_?.isEmpty)
			errors += "model not set"
		if (!errors.isEmpty)
			return Error(errors)

		val dim = setup.dim_?.get
		
		val conf = new Plate(
			obj = this,
			sLabel = setup.sLabel_?.get,
			model = setup.model_?.get)
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
	val model: PlateModel
) extends ObjConfig with Ordered[Plate] {
	def nRows: Int = model.nRows
	def nCols: Int = model.nCols
	def nWells: Int = nRows * nCols
	
	def state(states: StateMap): State = states(this).asInstanceOf[State]
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
	
	def location = obj.location_?.get
	def location_=(s: String) { obj.location_? = Some(s) }
	
	def wells = obj.dim_?.get.wells
}
*/