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

class TipAspirateEventBean extends EventBeanA[TipState] {
	@BeanProperty var src: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	
	protected def update(state0: TipState, states0: StateMap): TipState = {
		val volumeNew = state0.nVolume + LiquidVolume.l(volume)
		val liquid2 = states0(src) match {
			case liquid: Liquid => liquid
			case srcState: WellState => srcState.liquid
			case _ =>
				// FIXME: handle error better
				assert(false)
				return state0
		}
		new TipState(
			state0.conf,
			state0.model_?,
			state0.liquid + liquid2,
			volumeNew,
			state0.contamInside ++ liquid2.contaminants,
			LiquidVolume.max(state0.nContamInsideVolume, volumeNew),
			state0.contamOutside ++ liquid2.contaminants,
			state0.srcsEntered + liquid2,
			state0.destsEntered,
			WashIntensity.None,
			state0.cleanDegreePrev,
			WashIntensity.max(state0.cleanDegreePending, liquid2.group.cleanPolicy.exit)
		)
	}
}

object TipAspirateEventBean {
	def apply(tip: Tip, src: Well, volume: LiquidVolume): TipAspirateEventBean = {
		val bean = new TipAspirateEventBean
		bean.obj = tip.id
		bean.src = src.id
		bean.volume = volume.l.bigDecimal
		bean
	}
}

class TipDispenseEventBean extends EventBeanA[TipState] {
	@BeanProperty var dest: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var position: String = null
	
	protected def update(state0: TipState, states0: StateMap): TipState = {
		val volumeNew = state0.nVolume + LiquidVolume.l(volume)
		val liquidDest = states0(dest) match {
			case liquid: Liquid => liquid
			case srcState: WellState => srcState.liquid
			case _ =>
				// FIXME: handle error better
				assert(false)
				return state0
		}
		val pos = PipettePosition.withName(position)
		dispense(state0, LiquidVolume.l(volume), liquidDest, pos)
	}
	
	private def dispense(state0: TipState, nVolumeDisp: LiquidVolume, liquidDest: Liquid, pos: PipettePosition.Value): TipState = {
		pos match {
			case PipettePosition.WetContact => dispenseIn(state0, nVolumeDisp, liquidDest)
			case _ => dispenseFree(state0, nVolumeDisp)
		}
	}
	
	private def dispenseFree(state0: TipState, nVolume2: LiquidVolume): TipState = {
		val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(state0, nVolume2)
		state0.copy(
			liquid = liquid,
			nVolume = nVolume,
			cleanDegree = WashIntensity.None
		)
	}
	
	private def dispenseIn(state0: TipState, nVolume2: LiquidVolume, liquid2: Liquid): TipState = {
		val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(state0, nVolume2)
		state0.copy(
			liquid = liquid,
			nVolume = nVolume,
			contamOutside = state0.contamOutside ++ liquid2.contaminants,
			destsEntered = state0.destsEntered + liquid2,
			cleanDegree = WashIntensity.None,
			cleanDegreePending = WashIntensity.max(state0.cleanDegreePending, liquid2.group.cleanPolicy.exit)
		)
	}
	
	private def getLiquidAndVolumeAfterDispense(state0: TipState, nVolume2: LiquidVolume): Tuple2[Liquid, LiquidVolume] = {
		val nVolume3 = state0.nVolume - nVolume2
		if (nVolume3 < LiquidVolume.nl(1)) {
			(Liquid.empty, LiquidVolume.empty)
		}
		else {
			(state0.liquid, nVolume3)
		}
	}
}

object TipDispenseEventBean {
	def apply(tip: Tip, dest: Well, volume: LiquidVolume, pos: PipettePosition.Value): TipDispenseEventBean = {
		val bean = new TipDispenseEventBean
		bean.obj = tip.id
		bean.dest = dest.id
		bean.volume = volume.l.bigDecimal
		bean.position = pos.toString()
		bean
	}
}

class TipCleanEventBean extends EventBeanA[TipState] {
	@BeanProperty var degree: String = null
	
	protected def update(state0: TipState, states0: StateMap): TipState = {
		val cleanDegree = WashIntensity.withName(degree)
		state0.conf.createState0(None).copy(
			model_? = state0.model_?,
			cleanDegree = cleanDegree,
			cleanDegreePrev = cleanDegree,
			cleanDegreePending = WashIntensity.None
		)
	}
}

object TipCleanEventBean {
	def apply(tip: Tip, degree: WashIntensity.Value): TipCleanEventBean = {
		val bean = new TipCleanEventBean
		bean.obj = tip.id
		bean.degree = degree.toString
		bean
	}
}