package roboliq.commands.pipette

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
	type Setup = TipSetup
	type Config = TipConfigL2
	type State = TipStateL2
	
	override def compare(that: Tip): Int = this.index - that.index
	
	def createSetup() = new Setup
	
	def createConfigAndState0(setup: Setup): Either[Seq[String], Tuple2[Config, State]] = {
		val conf = new TipConfigL2(this, index)
		val state = new TipStateL2(conf, setup.sPermanentType_?, Liquid.empty, 0, Contamination.empty, 0, Nil, CleanDegree.None)
		Right(conf, state)
	}
	
	class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]
		
		def drop() {
			val st = state
			map(thisObj) = st.copy(sType_? = None)
		}
		
		def get(sType: String) {
			val st = state
			map(thisObj) = st.copy(sType_? = Some(sType))
		}
		
		def aspirate(liquid2: Liquid, nVolume2: Double) {
			val st = state
			val nVolumeNew = st.nVolume + nVolume2
			map(thisObj) = new TipStateL2(
				st.conf,
				st.sType_?,
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
	def state(state: StateBuilder): State = state.map(this).asInstanceOf[State]

	// For use in L3P_Pipette
	def stateWriter(map: HashMap[_ <: Obj, _ <: ObjState]) = new StateWriter(map.asInstanceOf[HashMap[Obj, ObjState]])
}

class TipConfigL2(
	val obj: Tip,
	val index: Int
) extends ObjConfig with Ordered[TipConfigL2] {
	// For use in L3P_Pipette
	def createState0(sType_? : Option[String]): TipStateL2 = {
		new TipStateL2(this, sType_?, Liquid.empty, 0, Contamination.empty, 0, Nil, CleanDegree.None)
	}

	override def compare(that: TipConfigL2): Int = this.index - that.index
}

case class TipStateL2(
	val conf: TipConfigL2,
	val sType_? : Option[String],
	val liquid: Liquid, 
	val nVolume: Double, 
	val contamInside: Contamination, 
	val nContamInsideVolume: Double,
	val destsEntered: List[Liquid],
	val cleanDegree: CleanDegree.Value
) extends ObjState with Ordered[TipStateL2] {
	override def compare(that: TipStateL2): Int = conf.obj.compare(that.conf.obj)
}

class TipSetup extends ObjSetup {
	var sPermanentType_? : Option[String] = None
}
