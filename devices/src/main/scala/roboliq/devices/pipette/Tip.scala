package roboliq.devices.pipette

import scala.collection.mutable.HashMap

import roboliq.common._

object CleanDegree extends Enumeration {
	val None, Light, Thorough, Decontaminate = Value
}

object PipettePosition extends Enumeration {
	val Free, WetContact, DryContact = Value
}

case class PipettePolicy(sName: String, pos: PipettePosition.Value)

/*
case class Site(val parent: Obj, val index: Int)

// TODO: add various speeds, such as entry, pump, exit
//case class PipettePolicy(val pos: PipettePosition.Value) // FIXME: remove this

object TipState {
	def apply(tip: Tip) = new TipState(tip, Liquid.empty, 0, Contamination.empty, 0, Nil, CleanDegree.None)
}
*/

class Tip(val index: Int) extends Obj with Ordered[Tip] {
	thisObj =>
	type ConfigL1 = TipConfigL1
	type ConfigL3 = TipConfigL3
	type StateL1 = TipStateL1
	type StateL3 = TipStateL3
	
	override def compare(that: Tip): Int = this.index - that.index
	
	def createConfigL1(c3: ConfigL3): Either[Seq[String], ConfigL1] = {
		Right(new TipConfigL1(index))
	}

	def createConfigL3() = new ConfigL3
	
	def createState0L1(state3: StateL3): Either[Seq[String], StateL1] = {
		Right(new TipStateL1(this, Liquid.empty, 0, Contamination.empty, 0, Nil, CleanDegree.None))
	}
	
	def createState0L3() = new StateL3
	
	class StateWriter(map: HashMap[Obj, Any]) {
		def state = map(thisObj).asInstanceOf[StateL1]
		
		def aspirate(liquid2: Liquid, nVolume2: Double) {
			val st = state
			val nVolumeNew = st.nVolume + nVolume2
			map(thisObj) = new TipStateL1(
				st.obj,
				st.liquid + liquid2,
				nVolumeNew,
				st.contamInside + liquid2,
				math.max(st.nContamInsideVolume, nVolumeNew),
				st.destsEntered,
				CleanDegree.None
			)
		}
		
		def dispense(nVolumeDisp: Double, liquidDest: Liquid, pos: PipettePosition.Value) {
			pos match {
				case PipettePosition.WetContact => dispenseIn(nVolumeDisp, liquidDest)
				case _ => dispenseFree(nVolumeDisp)
			}
		}
		
		def dispenseFree(nVolume2: Double) {
			val st = state
			map(thisObj) = st.copy(nVolume = st.nVolume - nVolume2, cleanDegree = CleanDegree.None)
		}
		
		def dispenseIn(nVolume2: Double, liquid2: Liquid) {
			val st = state
			map(thisObj) = st.copy(nVolume = st.nVolume - nVolume2, destsEntered = liquid2 :: st.destsEntered, cleanDegree = CleanDegree.None)
		}
		
		def clean(cleanDegree: CleanDegree.Value) {
			val st = state
			map(thisObj) = st.copy(destsEntered = Nil, cleanDegree = cleanDegree)
		}
		
		def mix(liquid2: Liquid, nVolume2: Double) {
			aspirate(liquid2, nVolume2)
			dispenseIn(nVolume2, liquid2)
		}
	}
	
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
	def state(state: StateBuilder): StateL1 = state.map(this).asInstanceOf[StateL1]
	def state(state: RobotState): StateL1 = state.map(this).asInstanceOf[StateL1]

	// For use in Compiler_PipetteCommand
	def stateWriter(map: HashMap[Tip, TipStateL1]) = new StateWriter(map.asInstanceOf[HashMap[Obj, Any]])
}

class TipConfigL1(
	val index: Int
) extends AbstractConfigL1

class TipConfigL3 extends AbstractConfigL3

case class TipStateL1(
	val obj: Tip,
	val liquid: Liquid, 
	val nVolume: Double, 
	val contamInside: Contamination, 
	val nContamInsideVolume: Double,
	val destsEntered: List[Liquid],
	val cleanDegree: CleanDegree.Value
) extends AbstractStateL1

class TipStateL3 extends AbstractStateL3
