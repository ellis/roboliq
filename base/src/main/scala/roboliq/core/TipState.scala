package roboliq.core

import scala.collection.mutable.HashMap
import scala.reflect.BeanProperty


/**
 * State of [[roboliq.core.Tip]].
 * Since [[roboliq.core.Tip]] is used to represent both a synringe and a tip,
 * this class combines state information for both the syringe and its possibly
 * attached tip.
 * 
 * @param conf the syringe/tip.
 * @param model_? optional tip model on this syringe.
 * @param src_? source well from which liquid was aspirated.
 * @param liquid liquid in the tip.
 * @param nVolume volume of liquid in the tip.
 */
case class TipState(
	val conf: Tip,
	val model_? : Option[TipModel],
	val src_? : Option[Well],
	val liquid: Liquid, // REFACTOR: change to VesselContent?
	val volume: LiquidVolume,
	val contamInside: Set[Contaminant.Value], 
	val nContamInsideVolume: LiquidVolume,
	val contamOutside: Set[Contaminant.Value],
	val srcsEntered: Set[Liquid],
	val destsEntered: Set[Liquid],
	val cleanDegree: CleanIntensity.Value,
	val cleanDegreePrev: CleanIntensity.Value,
	/** Intensity of cleaning that should be performed after leaving the current liquid group */
	val cleanDegreePending: CleanIntensity.Value
) extends Ordered[TipState] {
	override def compare(that: TipState): Int = conf.compare(that.conf)
}

/** Factory object for [[roboliq.core.TipState]]. */
object TipState {
	/** Create an initial state for `tip` with no liquid in it. */
	def createEmpty(tip: Tip) = TipState(
		conf = tip,
		model_? = tip.permanent_?,
		src_? = None,
		liquid = Liquid.empty,
		volume = LiquidVolume.empty,
		contamInside = Set(),
		nContamInsideVolume = LiquidVolume.empty,
		contamOutside = Set(),
		srcsEntered = Set(),
		destsEntered = Set(),
		cleanDegree = CleanIntensity.None,
		cleanDegreePrev = CleanIntensity.None,
		cleanDegreePending = CleanIntensity.None
	)
}

/**
 * Convenience class for modifying a tip's state.
 */
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
		val nVolumeNew = st.volume + nVolume2
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
			CleanIntensity.None,
			st.cleanDegreePrev,
			CleanIntensity.max(st.cleanDegreePending, liquid2.tipCleanPolicy.exit)
		))
	}
	
	def dispense(nVolumeDisp: LiquidVolume, liquidDest: Liquid, pos: PipettePosition.Value) {
		pos match {
			case PipettePosition.WetContact => dispenseIn(nVolumeDisp, liquidDest)
			case PipettePosition.Free => dispenseFree(nVolumeDisp)
			case PipettePosition.DryContact => dispenseFree(nVolumeDisp)
		}
	}
	
	def dispenseFree(nVolume2: LiquidVolume) {
		val st = state
		val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(nVolume2)
		set(st.copy(
			liquid = liquid,
			volume = nVolume,
			cleanDegree = CleanIntensity.None
		))
	}
	
	def dispenseIn(nVolume2: LiquidVolume, liquid2: Liquid) {
		val st = state
		val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(nVolume2)
		set(st.copy(
			liquid = liquid,
			volume = nVolume,
			contamOutside = st.contamOutside ++ liquid2.contaminants,
			destsEntered = st.destsEntered + liquid2,
			cleanDegree = CleanIntensity.None,
			cleanDegreePending = CleanIntensity.max(st.cleanDegreePending, liquid2.tipCleanPolicy.exit)
		))
	}
	
	private def getLiquidAndVolumeAfterDispense(nVolume2: LiquidVolume): Tuple2[Liquid, LiquidVolume] = {
		val st = state
		val nVolume3 = st.volume - nVolume2
		if (nVolume3 < LiquidVolume.nl(1)) {
			(Liquid.empty, LiquidVolume.empty)
		}
		else {
			(st.liquid, nVolume3)
		}
	}
	
	def clean(cleanDegree: CleanIntensity.Value) {
		val st = state
		set(TipState.createEmpty(o).copy(
			model_? = st.model_?,
			cleanDegree = cleanDegree,
			cleanDegreePrev = cleanDegree,
			cleanDegreePending = CleanIntensity.None
		))
	}
	
	def mix(well: Well, liquid2: Liquid, nVolume2: LiquidVolume) {
		aspirate(well, liquid2, nVolume2)
		dispenseIn(nVolume2, liquid2)
	}
}

/** Base class for tip events. */
abstract class TipEventBean extends EventBeanA[TipState] {
	protected def findState(id: String, query: StateQuery): Result[TipState] = {
		query.findTipState(id)
	}
}

/** Represents an aspiration event. */
class TipAspirateEventBean extends TipEventBean {
	/** ID of the source well. */
	@BeanProperty var src: String = null
	/** Volume in liters to aspirate. */
	@BeanProperty var volume: java.math.BigDecimal = null
	
	protected def update(state0: TipState, query: StateQuery): Result[TipState] = {
		for {
			liquid <- query.findSourceLiquid(src)
		} yield {
			val volumeNew = state0.volume + LiquidVolume.l(volume)
			val src_? = query.findWellPosition(src).toOption
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
				CleanIntensity.None,
				state0.cleanDegreePrev,
				CleanIntensity.max(state0.cleanDegreePending, liquid.tipCleanPolicy.exit)
			)
		}
	}
}

/** Factory object for [[roboliq.core.TipAspirateEventBean]]. */
object TipAspirateEventBean {
	/** Event to aspirate `volume` from `src` with `tip`. */
	def apply(tip: Tip, src: Well, volume: LiquidVolume): TipAspirateEventBean = {
		val bean = new TipAspirateEventBean
		bean.obj = tip.id
		bean.src = src.id
		bean.volume = volume.l.bigDecimal
		bean
	}
}

/** Represents an dispense event. */
class TipDispenseEventBean extends TipEventBean {
	/** ID of destination vessel. */
	@BeanProperty var dest: String = null
	/** Volume in liters to dispense. */
	@BeanProperty var volume: java.math.BigDecimal = null
	/** Position of the tip relative to liquid when dispensing (see [[roboliq.core.PipettePosition]]). */
	@BeanProperty var position: String = null
	
	protected def update(state0: TipState, query: StateQuery): Result[TipState] = {
		for {
			destState <- query.findWellState(dest)
		} yield {
			val liquidDest = destState.liquid
			val volumeNew = state0.volume + LiquidVolume.l(volume)
			val pos = PipettePosition.withName(position)
			dispense(state0, LiquidVolume.l(volume), liquidDest, pos)
		}
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
			volume = nVolume,
			cleanDegree = CleanIntensity.None
		)
	}
	
	private def dispenseIn(state0: TipState, nVolume2: LiquidVolume, liquid2: Liquid): TipState = {
		val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(state0, nVolume2)
		state0.copy(
			liquid = liquid,
			volume = nVolume,
			contamOutside = state0.contamOutside ++ liquid2.contaminants,
			destsEntered = state0.destsEntered + liquid2,
			cleanDegree = CleanIntensity.None,
			cleanDegreePending = CleanIntensity.max(state0.cleanDegreePending, liquid2.tipCleanPolicy.exit)
		)
	}
	
	private def getLiquidAndVolumeAfterDispense(state0: TipState, nVolume2: LiquidVolume): Tuple2[Liquid, LiquidVolume] = {
		val nVolume3 = state0.volume - nVolume2
		if (nVolume3 < LiquidVolume.nl(1)) {
			(Liquid.empty, LiquidVolume.empty)
		}
		else {
			(state0.liquid, nVolume3)
		}
	}
}

/** Factory object for [[roboliq.core.TipDispenseEventBean]]. */
object TipDispenseEventBean {
	/** Event to dispense `volume` from `tip` to `dest` at `pos`. */
	def apply(tip: Tip, dest: Well, volume: LiquidVolume, pos: PipettePosition.Value): TipDispenseEventBean = {
		val bean = new TipDispenseEventBean
		bean.obj = tip.id
		bean.dest = dest.id
		bean.volume = volume.l.bigDecimal
		bean.position = pos.toString()
		bean
	}
}

/** Represents a tip cleaning event. */
class TipCleanEventBean extends TipEventBean {
	/** Degree/intensity of cleaning (see [roboliq.core.CleanIntensity]]). */
	@BeanProperty var degree: String = null
	
	protected def update(state0: TipState, query: StateQuery): Result[TipState] = {
		val cleanDegree = CleanIntensity.withName(degree)
		Success(TipState.createEmpty(state0.conf).copy(
			model_? = state0.model_?,
			cleanDegree = cleanDegree,
			cleanDegreePrev = cleanDegree,
			cleanDegreePending = CleanIntensity.None
		))
	}
}

/** Factory object for [[roboliq.core.TipCleanEventBean]]. */
object TipCleanEventBean {
	/** Event to clean a `tip` with the given intensity `degree`. */
	def apply(tip: Tip, degree: CleanIntensity.Value): TipCleanEventBean = {
		val bean = new TipCleanEventBean
		bean.obj = tip.id
		bean.degree = degree.toString
		bean
	}
}