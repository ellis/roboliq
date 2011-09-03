package roboliq.common

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet


trait ObjSetup {
	def getLabel(kb: KnowledgeBase): String
}
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
	
	def state(states: StateMap): State = states(this).asInstanceOf[State]
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
	
	def createSetup() = new Setup(this)
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
				
		if (!errors.isEmpty)
			return Left(errors)

		val holderState = states(setup.holder_?.get).asInstanceOf[PlateStateL2]
		val conf = new WellConfigL2(
				obj = this,
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
	val holder: PlateConfigL2,
	val index: Int
) extends ObjConfig with Ordered[WellConfigL2] { thisConf =>
	type State = WellStateL2
	
	def iCol = index / holder.nRows
	def iRow = index % holder.nRows
	
	def state(states: StateMap) = obj.state(states)

	override def compare(that: WellConfigL2): Int = {
		holder.compare(that.holder) match {
			case 0 => index - that.index
			case n => n
		}
	}
	
	override def toString = holder.sLabel + ":" + (iCol+'A').asInstanceOf[Char] + (iRow+1)
}

case class WellStateL2(
	val conf: WellConfigL2,
	val plateState: PlateStateL2,
	val liquid: Liquid,
	val nVolume: Double
) extends ObjState

class WellSetup(val obj: Well) extends ObjSetup {
	var sLabel_? : Option[String] = None
	var holder_? : Option[Plate] = None
	var index_? : Option[Int] = None
	var bRequiresIntialLiq_? : Option[Boolean] = None
	var liquid_? : Option[Liquid] = None
	var nVolume_? : Option[Double] = None
	
	override def getLabel(kb: KnowledgeBase): String = {
		sLabel_? match {
			case Some(s) => s
			case None => toString
		}
		/*
		val sPlateLabel_? = holder_? match {
			case None => None
			case Some(holder) =>
				kb.getPlateSetup(holder).sLabel_?
		}
		(sPlateLabel_?, sLabel_?) match {
			case (Some(sPlate), Some(sWell)) =>
				sPlate + ":" + sLabel_?
		}*/
	}
	override def toString = sLabel_?.toString + " " + liquid_?
}
