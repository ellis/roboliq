package roboliq.common

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet


trait ObjSetup
trait ObjConfig
trait ObjState

trait ObjConfigImpl[T <: Obj] extends ObjConfig {
	val obj: T
}

trait ObjStateImpl[T <: ObjConfig] extends ObjState {
	val conf: T
}

abstract class Obj {
	type Setup <: ObjSetup
	type Config <: ObjConfig
	type State <: ObjState
	
	def createSetup(): Setup
	def createConfigAndState0(setup: Setup): Either[Seq[String], Tuple2[Config, State]]
	
	def getConfigL1(map31: ObjMapper): Option[Config] = map31.configL1(this) match { case Some(o) => Some(o.asInstanceOf[Config]); case None => None }
	def getConfigL3(map31: ObjMapper): Option[Setup] = map31.configL3(this) match { case Some(o) => Some(o.asInstanceOf[Setup]); case None => None }
	def getState0L1(map31: ObjMapper): Option[State] = map31.state0L3(this) match { case Some(o) => Some(o.asInstanceOf[State]); case None => None }
	def getState0L3(map31: ObjMapper): Option[Setup] = map31.state0L3(this) match { case Some(o) => Some(o.asInstanceOf[Setup]); case None => None }
}

sealed class Setting[T] {
	var default_? : Option[T] = None
	var user_? : Option[T] = None
	var possible: List[T] = Nil
	
	def get = user_? match {
		case None =>
			default_?.get
		case Some(o) =>
			o
	}
	
	def get_? : Option[T] = user_? match {
		case None =>
			default_?
		case Some(o) =>
			user_?
	} 
	
	def isDefined: Boolean = { user_?.isDefined || default_?.isDefined }
	def isEmpty: Boolean = !isDefined
}

class Well extends Obj { thisObj =>
	type Setup = WellSetup
	type Config = WellConfigL1
	type State = WellStateL1
	
	def createSetup() = new Setup
	def createConfigAndState0(setup: Setup): Either[Seq[String], Tuple2[Config, State]] = {
		val errors = new ArrayBuffer[String]
		
		// Check Config vars
		if (setup.holder_?.isEmpty)
			errors += "holder not set"
		if (setup.index_?.isEmpty)
			errors += "index not set"
			
		// Check state0 vars
		var liquid_? : Option[Liquid] = None
		var nVolume_? : Option[Double] = None
		
		if (setup.bRequiresIntialLiq_?.isEmpty || setup.bRequiresIntialLiq_?.get == false) {
			liquid_? = Some(Liquid.empty)
			nVolume_? = Some(0)
		}
		if (setup.liquid_?.isDefined)
			liquid_? = setup.liquid_?
		if (setup.nVolume_?.isDefined)
			nVolume_? = setup.nVolume_?
			
		if (liquid_?.isEmpty)
			errors += "liquid not set"
		if (nVolume_?.isEmpty)
			errors += "volume not set"
		
		if (!errors.isEmpty)
			return Left(errors)
			
		val conf = new WellConfigL1(
				obj = this,
				holder = setup.holder_?.get,
				index = setup.index_?.get)
		val state = new WellStateL1(
				conf = conf,
				liquid = liquid_?.get,
				nVolume = nVolume_?.get)
		
		Right(conf, state)
	}

	class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]
		
		def liquid = state.liquid
		//def liquid_=(liquid: Liquid) { map(thisObj) = state.copy(liquid = liquid) }
		
		def nVolume = state.nVolume
		//def nVolume_=(nVolume: Double) { map(thisObj) = state.copy(nVolume = nVolume) }

		def add(liquid2: Liquid, nVolume2: Double) {
			val st = state
			map(thisObj) = st.copy(liquid = st.liquid + liquid2, nVolume = st.nVolume + nVolume2)
		}
		
		def remove(nVolume2: Double) {
			val st = state
			map(thisObj) = st.copy(nVolume = st.nVolume - nVolume2)
		}
	}
	//def stateWriter(map: HashMap[ThisObj, StateL1]) = new StateWriter(this, map)
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
	def state(state: StateBuilder): State = state.map(this).asInstanceOf[State]
	def state(state: RobotState): State = state.map(this).asInstanceOf[State]
}

class WellConfigL1(
	val obj: Well,
	val holder: Plate,
	val index: Int
) extends ObjConfig with Ordered[WellConfigL1] {
	override def compare(that: WellConfigL1): Int = {
		val d1 = holder.hashCode() - that.holder.hashCode()
		if (d1 == 0) index - that.index
		else d1
	}
}

case class WellStateL1(
	val conf: WellConfigL1,
	val liquid: Liquid,
	val nVolume: Double
) extends ObjState

class WellSetup extends ObjSetup {
	var holder_? : Option[Plate] = None
	var index_? : Option[Int] = None
	var bRequiresIntialLiq_? : Option[Boolean] = None
	var liquid_? : Option[Liquid] = None
	var nVolume_? : Option[Double] = None
}

class Plate extends Obj {
	thisObj =>
	type Setup = PlateSetup
	type Config = PlateConfigL1
	type State = PlateStateL1
	
	def createSetup() = new Setup
	
	def createConfigAndState0(setup: Setup): Either[Seq[String], Tuple2[Config, State]] = {
		val errors = new ArrayBuffer[String]

		if (setup.dim_?.isEmpty)
			errors += "dimension not set"
		if (setup.location_?.isEmpty)
			errors += "location not set"
		if (!errors.isEmpty)
			return Left(errors)

		val dim = setup.dim_?.get
		
		val conf = new PlateConfigL1(
			obj = this,
			nRows = dim.nRows,
			nCols = dim.nCols,
			nWells = dim.nRows * dim.nCols,
			wells = dim.wells)
		val state = new PlateStateL1(
			conf = conf,
			location = setup.location_?.get)

		Right(conf, state)
	}

	class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]
		
		def location = state.location
		def location_=(location: String) { map(thisObj) = state.copy(location = location) }
	}
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
	def state(state: StateBuilder): State = state.map(this).asInstanceOf[State]
	def state(state: RobotState): State = state.map(this).asInstanceOf[State]
}

class PlateConfigL1(
	val obj: Plate,
	val nRows: Int,
	val nCols: Int,
	val nWells: Int,
	val wells: Seq[Well]
) extends ObjConfig

case class PlateStateL1(
	val conf: PlateConfigL1,
	val location: String
) extends ObjState


class PlateConfigDimensionL3(
	val nRows: Int,
	val nCols: Int,
	val wells: Seq[Well]
)

class PlateSetup extends ObjSetup {
	var dim_? : Option[PlateConfigDimensionL3] = None
	var location_? : Option[String] = None
}

class PlateProxy(kb: KnowledgeBase, obj: Plate) {
	val setup = kb.getPlateSetup(obj)
	def setDimension(rows: Int, cols: Int) {
		val nWells = rows * cols
		val wells = (0 until nWells).map(i => {
			val well = new Well
			kb.addWell(well)
			val wellSetup = kb.getWellSetup(well)
			wellSetup.index_? = Some(i)
			wellSetup.holder_? = Some(obj)
			well
		})
		val dim = new PlateConfigDimensionL3(rows, cols, wells.toSeq)
		setup.dim_? = Some(dim)
	}
	
	def location = setup.location_?.get
	def location_=(s: String) { setup.location_? = Some(s) }
	
	def wells = setup.dim_?.get.wells
}
