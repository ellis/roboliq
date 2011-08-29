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
	
	def getSetup(map31: ObjMapper): Option[Setup] = map31.configL4(this) match { case Some(o) => Some(o.asInstanceOf[Setup]); case None => None }
	def getConfigL2(map31: ObjMapper): Option[Config] = map31.configL2(this) match { case Some(o) => Some(o.asInstanceOf[Config]); case None => None }
	def getState0L2(map31: ObjMapper): Option[State] = map31.state0L4(this) match { case Some(o) => Some(o.asInstanceOf[State]); case None => None }
	
	def state(states: RobotState): State = states(this).asInstanceOf[State]
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
	type Config = WellConfigL2
	type State = WellStateL2
	
	def createSetup() = new Setup
	def createConfigAndState0(setup: Setup): Either[Seq[String], Tuple2[Config, State]] = {
		Left(Seq("well configs must be created separately"))
	}
	
	def createConfigAndState0(setup: Setup, states: scala.collection.Map[Obj, ObjState]): Either[Seq[String], Tuple2[Config, State]] = {
		val errors = new ArrayBuffer[String]
		
		// Check Config vars
		setup.holder_? match {
			case None => errors += "holder not set"
			case Some(holder) =>
				states.get(holder) match {
					case None => errors += "well's holder is not listed in state map"
					case Some(holderState) =>
				}
		}
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
				
		if (errors.isEmpty && setup.sLabel_?.isEmpty)
			errors += "label not set"
		
		if (!errors.isEmpty)
			return Left(errors)

		val holderState = states(setup.holder_?.get).asInstanceOf[PlateStateL2]
		val conf = new WellConfigL2(
				obj = this,
				sLabel = setup.sLabel_?.get,
				holder = holderState.conf,
				index = setup.index_?.get)
		val state = new WellStateL2(
				conf = conf,
				plateState = holderState,
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
	//def stateWriter(map: HashMap[ThisObj, StateL2]) = new StateWriter(this, map)
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
	def state(state: StateBuilder): State = state.map(this).asInstanceOf[State]
}

/*
class WellL3(val state: WellStateL2, val holder: PlateStateL2) extends Ordered[WellL3] {
	def conf: WellConfigL2 = state.conf
	def sLabel = conf.sLabel
	def index = conf.index
	def liquid = state.liquid
	def nVolume = state.nVolume
	
	override def compare(that: WellL3): Int = conf.compare(that.conf)
}

class WellL2(val conf: WellConfigL2, val holder: PlateConfigL2) extends Ordered[WellL2] {
	def obj = conf.obj
	def sLabel = conf.sLabel
	def index = conf.index
	
	override def compare(that: WellL2): Int = conf.compare(that.conf)
}

object WellL2 {
	def apply(well: WellConfigL2, states: RobotState) = new WellL2(well, well.holder.state(states).conf)
	def apply(well: WellL3) = new WellL2(well.conf, well.holder.conf)
}
*/

class WellConfigL2(
	val obj: Well,
	val sLabel: String,
	val holder: PlateConfigL2,
	val index: Int
) extends ObjConfig with Ordered[WellConfigL2] { thisConf =>
	type State = WellStateL2
	
	def iCol = index / holder.nRows
	def iRow = index % holder.nRows

	override def compare(that: WellConfigL2): Int = {
		holder.compare(that.holder) match {
			case 0 => index - that.index
			case n => n
		}
	}
	
	override def toString = sLabel
}

case class WellStateL2(
	val conf: WellConfigL2,
	val plateState: PlateStateL2,
	val liquid: Liquid,
	val nVolume: Double
) extends ObjState

class WellSetup extends ObjSetup {
	var sLabel_? : Option[String] = None
	var holder_? : Option[Plate] = None
	var index_? : Option[Int] = None
	var bRequiresIntialLiq_? : Option[Boolean] = None
	var liquid_? : Option[Liquid] = None
	var nVolume_? : Option[Double] = None
	
	override def toString = sLabel_?.toString + " " + liquid_?
}

class Plate extends Obj {
	thisObj =>
	type Setup = PlateSetup
	type Config = PlateConfigL2
	type State = PlateStateL2
	
	def createSetup() = new Setup
	
	def createConfigAndState0(setup: Setup): Either[Seq[String], Tuple2[Config, State]] = {
		val errors = new ArrayBuffer[String]

		if (setup.sLabel_?.isEmpty)
			errors += "label not set"
		if (setup.dim_?.isEmpty)
			errors += "dimension not set"
		if (setup.location_?.isEmpty)
			errors += "location not set"
		if (!errors.isEmpty)
			return Left(errors)

		val dim = setup.dim_?.get
		
		val conf = new PlateConfigL2(
			obj = this,
			sLabel = setup.sLabel_?.get,
			nRows = dim.nRows,
			nCols = dim.nCols,
			nWells = dim.nRows * dim.nCols,
			wells = dim.wells)
		val state = new PlateStateL2(
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
}

class PlateConfigL2(
	val obj: Plate,
	val sLabel: String,
	val nRows: Int,
	val nCols: Int,
	val nWells: Int,
	val wells: Seq[Well]
) extends ObjConfig with Ordered[PlateConfigL2] {
	override def compare(that: PlateConfigL2) = sLabel.compare(that.sLabel)
	override def toString = sLabel
}

case class PlateStateL2(
	val conf: PlateConfigL2,
	val location: String
) extends ObjState


class PlateConfigDimensionL4(
	val nRows: Int,
	val nCols: Int,
	val wells: Seq[Well]
)

class PlateSetup extends ObjSetup {
	var sLabel_? : Option[String] = None
	var dim_? : Option[PlateConfigDimensionL4] = None
	var location_? : Option[String] = None
}

class PlateProxy(kb: KnowledgeBase, obj: Plate) {
	val setup = kb.getPlateSetup(obj)
	
	def label = setup.sLabel_?.get
	def label_=(s: String) { setup.sLabel_? = Some(s) }
	
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
		val dim = new PlateConfigDimensionL4(rows, cols, wells.toSeq)
		setup.dim_? = Some(dim)
	}
	
	def location = setup.location_?.get
	def location_=(s: String) { setup.location_? = Some(s) }
	
	def wells = setup.dim_?.get.wells
}
