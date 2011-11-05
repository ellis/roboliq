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
	def createConfigAndState0(setup: Setup): Result[Tuple2[Config, State]]
	
	def getSetup(map31: ObjMapper): Option[Setup] = map31.setup(this) match { case Some(o) => Some(o.asInstanceOf[Setup]); case None => None }
	def getConfig(map31: ObjMapper): Option[Config] = map31.config(this) match { case Some(o) => Some(o.asInstanceOf[Config]); case None => None }
	def getState0(map31: ObjMapper): Option[State] = map31.state0(this) match { case Some(o) => Some(o.asInstanceOf[State]); case None => None }
	
	def state(states: StateMap): State = states(this).asInstanceOf[State]
	def stateOpt(states: StateMap): Option[State] = states.map.get(this).map(_.asInstanceOf[State])
	def stateRes(states: StateMap): Result[State] = Result.get(stateOpt(states), "missing state for object \""+toString+"\"")
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
	
	val setup = new Setup(this)
	def createSetup() = setup
	
	def createConfigAndState0(setup: Setup): Result[Tuple2[Config, State]] = {
		Error(Seq("well configs must be created separately"))
	}
	
	def createConfigAndState0(setup: Setup, states: StateMap): Result[Tuple2[Config, State]] = {
		val errors = new ArrayBuffer[String]
		
		// Check Config vars
		setup.holder_? match {
			case None => errors += "holder not set"
			case Some(holder) =>
				states.map.get(holder) match {
					case None => errors += "well's holder is not listed in state map"
					case _ =>
				}
		}
		val liquid_? : Option[Liquid] = setup.reagent_? match {
			case None => None
			case Some(reagent) =>
				states.map.get(reagent) match {
					case None =>
						errors += "well's reagent is not listed in state map"
						None
					case _ =>
						Some(reagent.state(states).conf.liquid)
				}
		}
		if (setup.index_?.isEmpty)
			errors += "index not set"
				
		if (setup.bRequiresIntialLiq_?.getOrElse(false) && liquid_?.isEmpty)
			errors += "must specify initial liquid for source wells"
			
		if (!errors.isEmpty)
			return Error(errors)
			
		val holderState = states(setup.holder_?.get).asInstanceOf[PlateStateL2]
		val nVolume = setup.nVolume_?.getOrElse(0.0)

		val conf = new WellConfigL2(
				obj = this,
				holder = holderState.conf,
				index = setup.index_?.get)
		val state = new WellStateL2(
				conf = conf,
				plateState = holderState,
				liquid = liquid_?.getOrElse(Liquid.empty),
				nVolume = nVolume)
		
		Success(conf, state)
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
	
	override def toString = setup.toString
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
	
	override def toString = holder.sLabel + ":" + (iRow+'A').asInstanceOf[Char] + (iCol+1)
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
	var reagent_? : Option[Reagent] = None
	var nVolume_? : Option[Double] = None
	
	override def getLabel(kb: KnowledgeBase): String = {
		val s = new StringBuilder
		holder_? match {
			case Some(holder) =>
				val holderSetup = kb.getPlateSetup(holder)
				s.append(holderSetup.getLabel(kb))
				s.append(':')
				index_? match {
					case Some(index) =>
						holderSetup.dim_? match {
							case None => s.append(index)
							case Some(dim) =>
								val iRow = index % dim.nRows
								val iCol = index / dim.nRows
								s.append((iRow+'A').asInstanceOf[Char])
								s.append(iCol+1)
						}
					case None =>
						s.append(toString)
				}
			case None =>
				s.append(toString)
		}
		s.toString
	}
	override def toString = sLabel_?.toString + " " + reagent_?
}
