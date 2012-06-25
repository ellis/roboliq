package roboliq.core

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.reflect.BeanProperty


/**
 * Base class for well states.
 * 
 * @param content Current vessel content.
 * @param bCheckVolume Whether it should be verified that the volume in this well doesn't go below 0.
 * When the initial volume of liquid in the well in unknown, then this should be `false`.
 * @param history List of events history for this well.
 */
abstract class WellState(
	val content: VesselContent,
	val bCheckVolume: Boolean,
	val history: List[EventBean]
) {
	/** Liquid in the well. */
	val liquid: Liquid = content.liquid
	/** Volume of liquid in well. */
	val nVolume: LiquidVolume = content.volume
	
	/** Prepend `event` to `history`, and set `content` and `bCheckVolume`. */
	def update(
		event: EventBean,
		content: VesselContent = content,
		bCheckVolume: Boolean = bCheckVolume
	): WellState
	
	override def toString: String = {
		List("liquid" -> liquid, "volume" -> nVolume).map(pair => pair._1+": "+pair._2).mkString("WellState(", ", ", ")")
	}
}

/**
 * State of [[roboliq.core.PlateWell]].
 * 
 * @param conf The `PlateWell`.
 */
class PlateWellState(
	val conf: PlateWell,
	content: VesselContent,
	bCheckVolume: Boolean,
	history: List[EventBean]
) extends WellState(content, bCheckVolume, history) {
	def update(
		event: EventBean,
		content: VesselContent = content,
		bCheckVolume: Boolean = bCheckVolume
	): PlateWellState = {
		new PlateWellState(
			conf, content, bCheckVolume, event :: history
		)
	}
}

/**
 * State of [[roboliq.core.Tube]].
 * 
 * @param obj The tube.
 * @param idPlate ID of the rack the tube is in.
 * @param row index of the tube's row on the rack (0-based).
 * @param col index of the tube's column on the rack (0-based).
 */
class TubeState(
	val obj: Tube,
	val idPlate: String,
	val row: Int,
	val col: Int,
	content: VesselContent,
	bCheckVolume: Boolean,
	history: List[EventBean]
) extends WellState(content, bCheckVolume, history) {
	def update(
		event: EventBean,
		content: VesselContent = content,
		bCheckVolume: Boolean = bCheckVolume
	): TubeState = {
		new TubeState(
			obj, idPlate, row, col, content, bCheckVolume, event :: history
		)
	}
	
	override def toString: String = {
		List(obj, idPlate, row, col, liquid, nVolume).mkString("TubeState(", ", ", ")")
	}
}

/** Represents a event that modifies [[roboliq.core.WellState]]. */
abstract class WellEventBean extends EventBeanA[WellState] {
	protected def findState(id: String, query: StateQuery): Result[WellState] = {
		query.findWellState(id)
	}
}

/** Represents the event of adding a substance to a well. */
class WellAddEventBean extends WellEventBean {
	/** ID of source well -- either `src` or `substance` must be set. */
	@BeanProperty var src: String = null
	/** ID of substance -- either `src` or `substance` must be set. */
	@BeanProperty var substance: String = null
	/**
	 * Volume to add.  Must be set and should be larger than 0.
	 * If substance is not a liquid, this will be the volume of water added.
	 */
	@BeanProperty var volume: java.math.BigDecimal = null
	/** Concentration of substance, if it's a powder. */
	@BeanProperty var conc: java.math.BigDecimal = null
	
	protected def update(state0: WellState, states0: StateQuery): Result[WellState] = {
		val v = LiquidVolume.l(volume)
		if (src != null) {
			for { wellState <- states0.findWellState(src) }
			yield {
				state0.update(this,
					content = state0.content.addContentByVolume(wellState.content, v)
				)
			}
		}
		else if (substance != null) {
			for {sub <- states0.findSubstance(substance)}
			yield {
				val content: VesselContent = sub match {
					case liquid: SubstanceLiquid =>
						state0.content.addLiquid(liquid, v)
					case _ =>
						Result.mustBeSet(conc, "conc") match {
							case Error(ls) => return Error(ls)
							case _ =>
						}
						val water = states0.findSubstance("water") match {
							case Error(ls) => return Error(ls)
							case Success(water: SubstanceLiquid) => water
							case _ => return Error("water must be a liquid")
						}
						val mol = BigDecimal(conc) * volume
						state0.content.addPowder(sub, mol).addLiquid(water, v)
				}
				state0.update(this,
					content = content
				)
			}
		}
		else {
			Error("`src` must be set")
		}
	}
}

/** Factory object for [[roboliq.core.WellEventBean]]. */
object WellAddEventBean {
	/** Event to . */
	def apply(well: Well2, src: Well2, volume: LiquidVolume): WellAddEventBean = {
		val bean = new WellAddEventBean
		bean.obj = well.id
		bean.src = src.id
		bean.volume = volume.l.bigDecimal
		bean
	}
}

/** Represents the event of adding powder to a well. */
class WellAddPowderEventBean extends WellEventBean {
	/** ID of powder substance in the database. */
	@BeanProperty var substance: String = null
	/** Volume in mol to add. */
	@BeanProperty var mol: java.math.BigDecimal = null
	
	protected def update(state0: WellState, states0: StateQuery): Result[WellState] = {
		for {
			_ <- Result.mustBeSet(substance, "substance")
			_ <- Result.mustBeSet(mol, "mol")
			substanceObj <- states0.findSubstance(substance)
		} yield {
			state0.update(this,
				content = state0.content.addPowder(substanceObj, mol)
			)
		}
	}
}

/** Represents the event of removing liquid from a well. */
class WellRemoveEventBean extends WellEventBean {
	/** Volume in liters to remove. */
	@BeanProperty var volume: java.math.BigDecimal = null
	
	protected def update(state0: WellState, states0: StateQuery): Result[WellState] = {
		val volumeNew = state0.nVolume - LiquidVolume.l(volume);
		for {
			_ <- Result.assert(volumeNew > LiquidVolume.empty || !state0.bCheckVolume, "tried to remove too much liquid from "+obj)
		} yield {
			state0.update(this,
				content = state0.content.removeVolume(LiquidVolume.l(volume))
			)
		}
	}
}

/** Factory object for [[roboliq.core.WellRemoveEventBean]]. */
object WellRemoveEventBean {
	/** Event to remove `volume` from `well`. */
	def apply(well: Well2, volume: LiquidVolume): WellRemoveEventBean = {
		val bean = new WellRemoveEventBean
		bean.obj = well.id
		bean.volume = volume.l.bigDecimal
		bean
	}
}

/**
 * Convenience class for modifying [[roboliq.core.WellState]].
 * 
 * @param id ID of well in database.
 * @param builder The state builder.
 */
class WellStateWriter(id: String, builder: StateBuilder) {
	def state = builder.findWellState(id).get
	
	private def set(state1: WellState) { builder.map(id) = state1 }
	
	def liquid = state.liquid
	
	def nVolume = state.nVolume

	def add(content2: VesselContent, nVolume2: LiquidVolume) {
		val st = state
		set(st.update(null, content = st.content.addContentByVolume(content2, nVolume2)))
	}
	
	def remove(nVolume2: LiquidVolume) {
		val st = state
		val nVolumeNew = st.nVolume - nVolume2;
		// TODO: more sophisticated checks should be made; let both minimum and maximum levels be set and issue errors rather than crashing
		if (st.bCheckVolume) {
			if (nVolumeNew < LiquidVolume.empty) {
				println("tried to remove too much liquid from "+id)
				//assert(false)
			}
		}
		set(st.update(null, content = st.content.removeVolume(nVolume2)))
	}
}

/** Represents a tube re-location event. */
class TubeLocationEventBean extends WellEventBean {
	/** ID of plate or rack. */
	@BeanProperty var location: String = null
	/** Index of row (0-based). */
	@BeanProperty var row: java.lang.Integer = null
	/** Index of column (0-based). */
	@BeanProperty var col: java.lang.Integer = null
	
	protected def update(state0: WellState, states0: StateQuery): Result[WellState] = {
		val st = state0.asInstanceOf[TubeState]
		for {
			_ <- Result.mustBeSet(location, "location")
			_ <- Result.mustBeSet(row, "row")
			_ <- Result.mustBeSet(col, "col")
		} yield {
			new TubeState(
				obj = st.obj,
				idPlate = location,
				row = row,
				col = col,
				content = st.content,
				bCheckVolume = st.bCheckVolume,
				history = this :: st.history
			)
		}
	}
}

/** Factory object for [[roboliq.core.TubeLocationEventBean]]. */
object TubeLocationEventBean {
	/** Event to relocate a `tube` to the given plate/rack `location` at the given `row` and `col`. */
	def apply(tube: Tube, location: String, row: Int, col: Int): TubeLocationEventBean = {
		val bean = new TubeLocationEventBean
		bean.obj = tube.id
		bean.location = location
		bean.row = row
		bean.col = col
		bean
	}
}
