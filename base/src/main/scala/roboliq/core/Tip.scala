package roboliq.core

import scala.collection.mutable.HashMap
import scala.reflect.BeanProperty

class TipModelBean extends Bean {
	@BeanProperty var id: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var volumeAspirateMin: LiquidVolume = null 
	//@BeanProperty var nVolumeWashExtra: LiquidVolume
	//@BeanProperty var nVolumeDeconExtra: LiquidVolume
}

case class TipModel(
	val id: String,
	val nVolume: LiquidVolume, 
	val nVolumeAspirateMin: LiquidVolume, 
	val nVolumeWashExtra: LiquidVolume,
	val nVolumeDeconExtra: LiquidVolume
)

class TipBean extends Bean {
	@BeanProperty var index: java.lang.Integer = null
	@BeanProperty var modelPermanent: TipModel = null
}

/*
class TipX(val index: Int, modelPermanent_? : Option[TipModel]) extends Ordered[Tip] {
	thisObj =>
	type Config = Tip
	type State = TipStateL2
	
	override def getLabel(kb: KnowledgeBase): String = toString
	val (conf0, state0) = _createConfigAndState0()
	
	override def compare(that: Tip): Int = this.index - that.index
	
	def createConfigAndState0(): Result[Tuple2[Config, State]] = {
		//val conf = new Tip(this, index)
		//val state = new TipStateL2(conf, modelPermanent_?, Liquid.empty, 0, Set(), 0, Set(), Set(), Set(), WashIntensity.None, WashIntensity.None, WashIntensity.None)
		Success(conf0, state0)
	}
	
	private def _createConfigAndState0(): Tuple2[Config, State] = {
		val conf = new Tip(this, index)
		val state = new TipStateL2(conf, modelPermanent_?, Liquid.empty, 0, Set(), 0, Set(), Set(), Set(), WashIntensity.None, WashIntensity.None, WashIntensity.None)
		(conf, state)
	} 
	
	class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]
		
		def drop() {
			val st = state
			map(thisObj) = state.conf.(None)
			//println("DROP tip "+thisObj.index+": sType = "+state.model_?)
		}
		
		def get(model: TipModel) {
			val st = state
			map(thisObj) = st.copy(model_? = Some(model))
			//println("tip "+thisObj.index+": sType = "+state.model_?)
		}
		
		def aspirate(liquid2: Liquid, nVolume2: LiquidVolume) {
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
		
		def dispense(nVolumeDisp: LiquidVolume, liquidDest: Liquid, pos: PipettePosition.Value) {
			pos match {
				case PipettePosition.WetContact => dispenseIn(nVolumeDisp, liquidDest)
				case _ => dispenseFree(nVolumeDisp)
			}
		}
		
		def dispenseFree(nVolume2: LiquidVolume) {
			val st = state
			val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(nVolume2)
			map(thisObj) = st.copy(
				liquid = liquid,
				nVolume = nVolume,
				cleanDegree = WashIntensity.None
			)
		}
		
		def dispenseIn(nVolume2: LiquidVolume, liquid2: Liquid) {
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
		
		private def getLiquidAndVolumeAfterDispense(nVolume2: LiquidVolume): Tuple2[Liquid, LiquidVolume] = {
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
			map(thisObj) = st.conf.(None).copy(
				model_? = st.model_?,
				cleanDegree = cleanDegree,
				cleanDegreePrev = cleanDegree,
				cleanDegreePending = WashIntensity.None
			)
		}
		
		def mix(liquid2: Liquid, nVolume2: LiquidVolume) {
			aspirate(liquid2, nVolume2)
			dispenseIn(nVolume2, liquid2)
		}
	}
	
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
	def state(state: StateBuilder): State = state.map(this).asInstanceOf[State]

	// For use in L3P_Pipette
	def stateWriter(map: HashMap[_ <: Obj, _ <: ObjState]) = new StateWriter(map.asInstanceOf[HashMap[Obj, ObjState]])
	
	override def toString = "Tip"+(index+1)
}*/

class Tip(
	val index: Int
) extends Ordered[Tip] {
	val id = "TIP"+index
	
	def state(states: StateMap): TipState = states(this.id).asInstanceOf[TipState]
	def stateWriter(builder: StateBuilder): TipStateWriter = new TipStateWriter(this, builder)

	override def compare(that: Tip) = index - that.index
	override def toString = id

	// For use by TipStateWriter
	def createState0(model_? : Option[TipModel]): TipState = {
		new TipState(this, model_?, Liquid.empty, LiquidVolume.l(0), Set(), LiquidVolume.l(0), Set(), Set(), Set(), WashIntensity.None, WashIntensity.None, WashIntensity.None)
	}
}

object TipSet {
	def toDebugString(set: Set[Tip]): String = toDebugString(collection.immutable.SortedSet(set.toSeq : _*))
	def toDebugString(set: collection.immutable.SortedSet[Tip]): String = toDebugString(set.toSeq)
	def toDebugString(seq: Seq[Tip]): String = {
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

case class TipState(
	val conf: Tip,
	val model_? : Option[TipModel],
	val liquid: Liquid, 
	val nVolume: LiquidVolume, 
	val contamInside: Set[Contaminant.Value], 
	val nContamInsideVolume: LiquidVolume,
	val contamOutside: Set[Contaminant.Value],
	val srcsEntered: Set[Liquid],
	val destsEntered: Set[Liquid],
	val cleanDegree: WashIntensity.Value,
	val cleanDegreePrev: WashIntensity.Value,
	/** Intensity of cleaning that should be performed after leaving the current liquid group */
	val cleanDegreePending: WashIntensity.Value
) extends Ordered[TipState] {
	override def compare(that: TipState): Int = conf.compare(that.conf)
}

class TipStateWriter(o: Tip, builder: StateBuilder) {
	def state = builder.map(o.id).asInstanceOf[TipState]
	
	private def set(state1: TipState) { builder.map(o.id) = state1 } 
	
	def drop() {
		set(state.conf.createState0(None))
		//println("DROP tip "+thisObj.index+": sType = "+state.model_?)
	}
	
	def get(model: TipModel) {
		val st = state
		set(st.copy(model_? = Some(model)))
		//println("tip "+thisObj.index+": sType = "+state.model_?)
	}
	
	def aspirate(liquid2: Liquid, nVolume2: LiquidVolume) {
		val st = state
		val nVolumeNew = st.nVolume + nVolume2
		set(new TipState(
			st.conf,
			st.model_?,
			st.liquid + liquid2,
			nVolumeNew,
			st.contamInside ++ liquid2.contaminants,
			LiquidVolume.max(st.nContamInsideVolume, nVolumeNew),
			st.contamOutside ++ liquid2.contaminants,
			st.srcsEntered + liquid2,
			st.destsEntered,
			WashIntensity.None,
			st.cleanDegreePrev,
			WashIntensity.max(st.cleanDegreePending, liquid2.group.cleanPolicy.exit)
		))
	}
	
	def dispense(nVolumeDisp: LiquidVolume, liquidDest: Liquid, pos: PipettePosition.Value) {
		pos match {
			case PipettePosition.WetContact => dispenseIn(nVolumeDisp, liquidDest)
			case _ => dispenseFree(nVolumeDisp)
		}
	}
	
	def dispenseFree(nVolume2: LiquidVolume) {
		val st = state
		val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(nVolume2)
		set(st.copy(
			liquid = liquid,
			nVolume = nVolume,
			cleanDegree = WashIntensity.None
		))
	}
	
	def dispenseIn(nVolume2: LiquidVolume, liquid2: Liquid) {
		val st = state
		val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(nVolume2)
		set(st.copy(
			liquid = liquid,
			nVolume = nVolume,
			contamOutside = st.contamOutside ++ liquid2.contaminants,
			destsEntered = st.destsEntered + liquid2,
			cleanDegree = WashIntensity.None,
			cleanDegreePending = WashIntensity.max(st.cleanDegreePending, liquid2.group.cleanPolicy.exit)
		))
	}
	
	private def getLiquidAndVolumeAfterDispense(nVolume2: LiquidVolume): Tuple2[Liquid, LiquidVolume] = {
		val st = state
		val nVolume3 = st.nVolume - nVolume2
		if (nVolume3 < LiquidVolume.nl(1)) {
			(Liquid.empty, LiquidVolume.empty)
		}
		else {
			(st.liquid, nVolume3)
		}
	}
	
	def clean(cleanDegree: WashIntensity.Value) {
		val st = state
		set(st.conf.createState0(None).copy(
			model_? = st.model_?,
			cleanDegree = cleanDegree,
			cleanDegreePrev = cleanDegree,
			cleanDegreePending = WashIntensity.None
		))
	}
	
	def mix(liquid2: Liquid, nVolume2: LiquidVolume) {
		aspirate(liquid2, nVolume2)
		dispenseIn(nVolume2, liquid2)
	}
}
