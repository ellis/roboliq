package roboliq.commands.pipette

import scala.collection.mutable.HashMap

import roboliq.common._


class Tip(val index: Int) extends Obj with Ordered[Tip] {
	thisObj =>
	type Setup = TipSetup
	type Config = TipConfigL2
	type State = TipStateL2
	
	override def compare(that: Tip): Int = this.index - that.index
	
	def createSetup() = new Setup(this)
	
	def createConfigAndState0(setup: Setup): Result[Tuple2[Config, State]] = {
		val conf = new TipConfigL2(this, index)
		val state = new TipStateL2(conf, setup.modelPermanent_?, Liquid.empty, 0, Set(), 0, Set(), Set(), Set(), WashIntensity.None, WashIntensity.None, WashIntensity.None)
		Success(conf, state)
	}
	
	class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]
		
		def drop() {
			val st = state
			map(thisObj) = state.conf.createState0(None)
			//println("DROP tip "+thisObj.index+": sType = "+state.model_?)
		}
		
		def get(model: TipModel) {
			val st = state
			map(thisObj) = st.copy(model_? = Some(model))
			//println("tip "+thisObj.index+": sType = "+state.model_?)
		}
		
		def aspirate(liquid2: Liquid, nVolume2: Double) {
			val st = state
			val nVolumeNew = st.nVolume + nVolume2
			map(thisObj) = new TipStateL2(
				st.conf,
				st.model_?,
				st.liquid + liquid2,
				nVolumeNew,
				st.contamInside ++ liquid2.contaminants,
				math.max(st.nContamInsideVolume, nVolumeNew),
				st.contamOutside ++ liquid2.contaminants,
				st.srcsEntered + liquid2,
				st.destsEntered,
				WashIntensity.None,
				st.cleanDegreePrev,
				WashIntensity.max(st.cleanDegreePending, liquid2.group.cleanPolicy.exit)
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
			val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(nVolume2)
			map(thisObj) = st.copy(
				liquid = liquid,
				nVolume = nVolume,
				cleanDegree = WashIntensity.None
			)
		}
		
		def dispenseIn(nVolume2: Double, liquid2: Liquid) {
			val st = state
			val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(nVolume2)
			map(thisObj) = st.copy(
				liquid = liquid,
				nVolume = nVolume,
				contamOutside = st.contamOutside ++ liquid2.contaminants,
				destsEntered = st.destsEntered + liquid2,
				cleanDegree = WashIntensity.None,
				cleanDegreePending = WashIntensity.max(st.cleanDegreePending, liquid2.group.cleanPolicy.exit)
			)
		}
		
		private def getLiquidAndVolumeAfterDispense(nVolume2: Double): Tuple2[Liquid, Double] = {
			val st = state
			val nVolume3 = st.nVolume - nVolume2
			if (math.abs(nVolume3) < 0.001) {
				(Liquid.empty, 0.0)
			}
			else {
				(st.liquid, nVolume3)
			}
		}
		
		def clean(cleanDegree: WashIntensity.Value) {
			val st = state
			map(thisObj) = st.conf.createState0(None).copy(
				model_? = st.model_?,
				cleanDegree = cleanDegree,
				cleanDegreePrev = cleanDegree,
				cleanDegreePending = WashIntensity.None
			)
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
	
	override def toString = "Tip"+(index+1)
}

class TipConfigL2(
	val obj: Tip,
	val index: Int
) extends ObjConfig with Ordered[TipConfigL2] {
	def state(states: StateMap) = obj.state(states)
	// For use in L3P_Pipette
	def createState0(model_? : Option[TipModel]): TipStateL2 = {
		new TipStateL2(this, model_?, Liquid.empty, 0, Set(), 0, Set(), Set(), Set(), WashIntensity.None, WashIntensity.None, WashIntensity.None)
	}

	override def compare(that: TipConfigL2): Int = this.index - that.index
	
	override def toString = "Tip"+(index+1)
}

case class TipStateL2(
	val conf: TipConfigL2,
	val model_? : Option[TipModel],
	val liquid: Liquid, 
	val nVolume: Double, 
	val contamInside: Set[Contaminant.Value], 
	val nContamInsideVolume: Double,
	val contamOutside: Set[Contaminant.Value],
	val srcsEntered: Set[Liquid],
	val destsEntered: Set[Liquid],
	val cleanDegree: WashIntensity.Value,
	val cleanDegreePrev: WashIntensity.Value,
	/** Intensity of cleaning that should be performed after leaving the current liquid group */
	val cleanDegreePending: WashIntensity.Value
) extends ObjState with Ordered[TipStateL2] {
	override def compare(that: TipStateL2): Int = conf.obj.compare(that.conf.obj)
}

class TipSetup(val obj: Tip) extends ObjSetup {
	var modelPermanent_? : Option[TipModel] = None

	override def getLabel(kb: KnowledgeBase): String = obj.toString
}

object TipSet {
	def toDebugString(set: Set[TipConfigL2]): String = toDebugString(collection.immutable.SortedSet(set.toSeq : _*))
	def toDebugString(set: collection.immutable.SortedSet[TipConfigL2]): String = toDebugString(set.toSeq)
	def toDebugString(seq: Seq[TipConfigL2]): String = {
		if (seq.isEmpty)
			"NoTips"
		else {
			var indexPrev = -1
			var indexLast = -1
			val l = seq.foldLeft(Nil: List[Tuple2[Int, Int]]) { (acc, tip) => {
				acc match {
					case Nil => List((tip.index, tip.index))
					case first :: rest =>
						if (tip.index == first._2 + 1)
							(first._1, tip.index) :: rest
						else
							(tip.index, tip.index) :: acc
				}
			}}
			val ls = l.reverse.map(pair => {
				if (pair._1 == pair._2) (pair._1 + 1).toString
				else (pair._1 + 1) + "-" + (pair._2 + 1)
			})
			ls.mkString("Tip", ",", "")
		}
	}
}
