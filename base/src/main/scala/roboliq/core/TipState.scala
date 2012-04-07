package roboliq.core

import scala.collection.mutable.HashMap
import scala.reflect.BeanProperty


case class TipState(
	val conf: Tip,
	val model_? : Option[TipModel],
	val src_? : Option[Well],
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

object TipState {
	def createEmpty(tip: Tip) = TipState(
		conf = tip,
		model_? = tip.modelPermanent_?,
		src_? = None,
		liquid = Liquid.empty,
		nVolume = LiquidVolume.empty,
		contamInside = Set(),
		nContamInsideVolume = LiquidVolume.empty,
		contamOutside = Set(),
		srcsEntered = Set(),
		destsEntered = Set(),
		cleanDegree = WashIntensity.None,
		cleanDegreePrev = WashIntensity.None,
		cleanDegreePending = WashIntensity.None
	)
}

class TipStateWriter(o: Tip, builder: StateBuilder) {
	def state = builder.findTipState(o.id).get
	
	private def set(state1: TipState) { builder.map(o.id) = state1 } 
	
	def drop() {
		set(TipState.createEmpty(o))
		//println("DROP tip "+thisObj.index+": sType = "+state.model_?)
	}
	
	def get(model: TipModel) {
		val st = state
		set(st.copy(model_? = Some(model)))
		//println("tip "+thisObj.index+": sType = "+state.model_?)
	}
	
	def aspirate(src: Well, liquid2: Liquid, nVolume2: LiquidVolume) {
		val st = state
		val nVolumeNew = st.nVolume + nVolume2
		set(new TipState(
			st.conf,
			st.model_?,
			Some(src),
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
		set(TipState.createEmpty(o).copy(
			model_? = st.model_?,
			cleanDegree = cleanDegree,
			cleanDegreePrev = cleanDegree,
			cleanDegreePending = WashIntensity.None
		))
	}
	
	def mix(well: Well, liquid2: Liquid, nVolume2: LiquidVolume) {
		aspirate(well, liquid2, nVolume2)
		dispenseIn(nVolume2, liquid2)
	}
}

class TipAspirateEventBean extends EventBeanA[TipState] {
	@BeanProperty var src: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	
	protected def update(state0: TipState, states0: StateMap): TipState = {
		val volumeNew = state0.nVolume + LiquidVolume.l(volume)
		val src_? = states0.findWell(src).toOption
		val liquid = states0.findLiquid(src) match {
			case Success(liquid) => liquid
			case Error(ls) =>
				states0.findWellState(src) match {
					case Success(wellState) => wellState.liquid
					case Error(ls) => assert(false); null
				}
		}
		new TipState(
			state0.conf,
			state0.model_?,
			src_?,
			state0.liquid + liquid,
			volumeNew,
			state0.contamInside ++ liquid.contaminants,
			LiquidVolume.max(state0.nContamInsideVolume, volumeNew),
			state0.contamOutside ++ liquid.contaminants,
			state0.srcsEntered + liquid,
			state0.destsEntered,
			WashIntensity.None,
			state0.cleanDegreePrev,
			WashIntensity.max(state0.cleanDegreePending, liquid.group.cleanPolicy.exit)
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
	/*def apply(tip: String, src: Well, volume: LiquidVolume): TipAspirateEventBean = {
		val bean = new TipAspirateEventBean
		bean.obj = tip
		bean.src = src.id
		bean.volume = volume.l.bigDecimal
		bean
	}*/
}

class TipDispenseEventBean extends EventBeanA[TipState] {
	@BeanProperty var dest: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var position: String = null
	
	protected def update(state0: TipState, states0: StateMap): TipState = {
		val volumeNew = state0.nVolume + LiquidVolume.l(volume)
		val liquidDest = states0.findWellState(dest) match {
			case Success(destState) => destState.liquid
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
		TipState.createEmpty(state0.conf).copy(
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