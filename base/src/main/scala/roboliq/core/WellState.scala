package roboliq.core

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.reflect.BeanProperty


abstract class WellState(
	//val well: Well,
	val content: VesselContent,
	/** Make sure that volume doesn't go below 0 */
	val bCheckVolume: Boolean,
	val history: List[EventBean]
) {
	val liquid: Liquid = content.liquid
	val nVolume: LiquidVolume = content.volume
	
	def update(
		event: EventBean,
		content: VesselContent = content,
		bCheckVolume: Boolean = bCheckVolume
	): WellState
	
	override def toString: String = {
		List("liquid" -> liquid, "volume" -> nVolume).map(pair => pair._1+": "+pair._2).mkString("WellState(", ", ", ")")
	}
}

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

abstract class WellEventBean extends EventBeanA[WellState] {
	protected def findState(id: String, query: StateQuery): Result[WellState] = {
		query.findWellState(id)
	}
}

class WellAddEventBean extends WellEventBean {
	@BeanProperty var src: String = null
	@BeanProperty var substance: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var conc: java.math.BigDecimal = null
	
	protected def update(state0: WellState, states0: StateQuery): Result[WellState] = {
		if (src != null) {
			for { wellState <- states0.findWellState(src) }
			yield {
				state0.update(this,
					content = state0.content.addContentByVolume(wellState.content, LiquidVolume.l(volume))
				)
			}
		}
		else if (substance != null) {
			for {sub <- states0.findSubstance(substance)}
			yield {
				val content: VesselContent = sub match {
					case liquid: SubstanceLiquid =>
						state0.content.addLiquid(liquid, LiquidVolume.l(volume))
					case _ => assert(false); null
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

object WellAddEventBean {
	def apply(well: Well, src: Well, volume: LiquidVolume): WellAddEventBean = {
		val bean = new WellAddEventBean
		bean.obj = well.id
		bean.src = src.id
		bean.volume = volume.l.bigDecimal
		bean
	}
}

class WellAddPowderEventBean extends WellEventBean {
	@BeanProperty var substance: String = null
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

class WellRemoveEventBean extends WellEventBean {
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

object WellRemoveEventBean {
	def apply(well: Well, volume: LiquidVolume): WellRemoveEventBean = {
		val bean = new WellRemoveEventBean
		bean.obj = well.id
		bean.volume = volume.l.bigDecimal
		bean
	}
}

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

class TubeLocationEventBean extends WellEventBean {
	@BeanProperty var location: String = null
	@BeanProperty var row: java.lang.Integer = null
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

object TubeLocationEventBean {
	def apply(tube: Tube, location: String, row: Int, col: Int): TubeLocationEventBean = {
		val bean = new TubeLocationEventBean
		bean.obj = tube.id
		bean.location = location
		bean.row = row
		bean.col = col
		bean
	}
}

/*sealed trait WellHistoryItem

/** Represents the addition of a substance to a well's history in YAML */
case class WellHistoryAdd(
	var substance: Liquid,
	var volume: LiquidVolume
) extends WellHistoryItem
*/

/*
class Well extends Obj { thisObj =>
	type Config = WellConfigL2
	type State = WellStateL2
	
	var sLabel_? : Option[String] = None
	var holder_? : Option[PlateObj] = None
	var index_? : Option[Int] = None
	var bRequiresIntialLiq_? : Option[Boolean] = None
	var reagent_? : Option[Reagent] = None
	var nVolume_? : Option[LiquidVolume] = None
	
	override def getLabel(kb: KnowledgeBase): String = {
		val s = new StringBuilder
		holder_? match {
			case Some(holder) =>
				s.append(holder.getLabel(kb))
				s.append(':')
				index_? match {
					case Some(index) =>
						holder.dim_? match {
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

	def createConfigAndState0(): Result[Tuple2[Config, State]] = {
		Error(Seq("well configs must be created separately"))
	}
	
	def createConfigAndState0(states: StateMap): Result[Tuple2[Config, State]] = {
		val errors = new ArrayBuffer[String]
		
		// Check Config vars
		holder_? match {
			case None => errors += "holder not set"
			case Some(holder) =>
				states.map.get(holder) match {
					case None => errors += "well's holder is not listed in state map"
						states.map.foreach(println)
					case _ =>
				}
		}
		val liquid_? : Option[Liquid] = reagent_? match {
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
		if (index_?.isEmpty)
			errors += "index not set"
				
		if (bRequiresIntialLiq_?.getOrElse(false) && liquid_?.isEmpty)
			errors += "must specify initial liquid for source wells"
			
		if (!errors.isEmpty)
			return Error(errors)
		
		val holder = holder_?.get
		val holderState = states(holder).asInstanceOf[PlateStateL2]
		val bInitialVolumeKnown = nVolume_?.isDefined
		val nVolume = nVolume_?.getOrElse(0.0)
		val bCheckVolume = (bInitialVolumeKnown || bRequiresIntialLiq_? == Some(false))

		val conf = new WellConfigL2(
				obj = this,
				holder = holderState.conf,
				index = index_?.get,
				bInitialVolumeKnown = bInitialVolumeKnown)
		val state = new WellStateL2(
				conf = conf,
				plateState = holderState,
				liquid = liquid_?.getOrElse(Liquid.empty),
				nVolume = nVolume,
				bCheckVolume = bCheckVolume)
		
		Success(conf, state)
	}

	//def stateWriter(map: HashMap[ThisObj, StateL2]) = new StateWriter(this, map)
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
	
	override def toString = sLabel_?.toString + " " + reagent_?
}

class WellConfigL2(
	val obj: Well,
	val holder: Plate,
	val index: Int,
	val bInitialVolumeKnown: Boolean
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
	val nVolume: LiquidVolume,
	/** Make sure that volume doesn't go below 0 */
	val bCheckVolume: Boolean
) extends ObjState
*/