package roboliq.common

import scala.collection.mutable.ArrayBuffer


abstract class ReagentPipettingFamily

class Reagent extends Obj { thisObj =>
	type Setup = ReagentSetup
	type Config = ReagentConfig
	type State = ReagentState
	
	def createSetup() = new Setup(this)
	def createConfigAndState0(setup: Setup): Either[Seq[String], Tuple2[Config, State]] = {
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

		val holderState = states(setup.holder_?.get).asInstanceOf[PlateState]
		val conf = new ReagentConfig(
				obj = this,
				holder = holderState.conf,
				index = setup.index_?.get)
		val state = new ReagentState(
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
	//def stateWriter(map: HashMap[ThisObj, State]) = new StateWriter(this, map)
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
}

class ReagentSetup(val obj: Reagent) extends ObjSetup {
	var sLabel_? : Option[String] = None
	var pipettingFamily_? : Option[ReagentPipettingFamily] = None
	var washIntensityBeforeAspirate_? : Option[WashIntensity.Value] = None
	var bReplaceTipsBeforeAspirate_? : Option[Boolean] = None
	var contaminants = Set[Contaminant.Value]()
	
	override def getLabel(kb: KnowledgeBase): String = {
		sLabel_? match {
			case Some(s) => s
			case None => toString
		}
	}
}

class ReagentConfig(
	val obj: Reagent,
	val sLabel: String,
	val liquid: Liquid,
) extends ObjConfig with Ordered[ReagentConfig] {
	type State = ReagentState
	
	def iCol = index / holder.nRows
	def iRow = index % holder.nRows
	
	def state(states: StateMap) = obj.state(states)

	override def compare(that: ReagentConfig): Int = {
		holder.compare(that.holder) match {
			case 0 => index - that.index
			case n => n
		}
	}
	
	override def toString = holder.sLabel + ":" + (iCol+'A').asInstanceOf[Char] + (iRow+1)
}

case class ReagentState extends ObjState
