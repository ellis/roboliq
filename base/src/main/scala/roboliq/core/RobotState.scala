package roboliq.core

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet


/**
 * Implementation of [[roboliq.core.StateQuery]] and base class for
 * both an immutable interface ([[scala.core.RobotState]]) and a mutable one ([[scala.core.StateBuilder]]). 
 */
abstract class StateMap(ob: ObjBase) extends StateQuery {
	/** Map from object ID to object state */
	val map: collection.Map[String, Object]
	def apply(id: String) = map(id)
	
	def toDebugString: String = {
		/*val b = new StringBuilder
		val objs = map.keys
		val shown = new HashSet[Obj]

		val plates = objs.collect { case o: PlateObj => o }
		if (!plates.isEmpty) {
			b.append("Plates:\n")
			for (plate <- plates) {
				val plateState = plate.state(this)
				b.append("\t").append(plateState.conf.sLabel).append(":\n")
				val wellStates = plateState.conf.wells.map(_.state(this))
				val wells = wellStates.map(_.conf)
				val liquids = wellStates.map(_.liquid.sName)
				val volumes = wellStates.map(_.nVolume)
				b.append("\t\t").append(Command.getWellsDebugString(wells)).append('\n')
				b.append("\t\t").append(Command.getSeqDebugString(liquids)).append('\n')
				b.append("\t\t").append(Command.getSeqDebugString(volumes)).append('\n')
			}
		}
			
		b.toString*/
		map.map(_.toString).mkString("\n")
	}

	def findTipModel(id: String): Result[TipModel] = ob.findTipModel(id)
	
	def findSubstance(id: String): Result[Substance] = ob.findSubstance(id)
	def findLiquid(id: String): Result[Liquid] = ob.findLiquid(id)
	def findTip(id: String): Result[Tip] = ob.findTip(id)
	def findPlate(id: String): Result[Plate] = ob.findPlate(id)
	//def findWell(id: String): Result[Well] = ob.findWell(id)
	
	def findTipState(id: String): Result[TipState] = {
		map.get(id) match {
			case Some(state) => Success(state.asInstanceOf[TipState])
			case None =>
				ob.findTipState(id)
		}
	}
	
	def findWellState(id: String): Result[WellState] = {
		//println("StateBuilder.findWellState: "+id)
		val s = map.get(id) match {
			case Some(state) => Success(state.asInstanceOf[WellState])
			case None => ob.findWellState(id)
		}
		//println("  "+s)
		s
	}
	
	def findWellPosition(id: String): Result[Well2] = {
		ob.findWell2(id)
	}
	
	def expandIdList(ids: String): Result[List[String]] =
		WellSpecParser.parseToIds(ids, ob)
	
	def mapIdToWell2List(id: String): Result[List[Well2]] = {
		ob.findWell2List(id)
	}
	
	def mapIdsToWell2Lists(ids: String): Result[List[List[Well2]]] = {
		for {
			lId <- WellSpecParser.parseToIds(ids, ob)
			ll <- Result.mapOver(lId)(mapIdToWell2List)
		} yield {
			ll
		}
	}
	
	def findDestWells(ids: String): Result[List[Well2]] = {
		for {
			ℓid <- WellSpecParser.parseToIds(ids, ob)
			ℓwell <- Result.mapOver(ℓid)(ob.findWell2)
		} yield {
			ℓwell
		}
	}
}

class RobotState(ob: ObjBase, val map: Map[String, Object]) extends StateMap(ob) {
	def filterByValueType[State <: Object](implicit m: Manifest[State]): Map[String, State] = {
		map.filter(pair => m.erasure.isInstance(pair._2)).mapValues(_.asInstanceOf[State])
	}
	
	def toBuilder: StateBuilder =
		new StateBuilder(ob, HashMap[String, Object](map.toSeq : _*))
}

class StateBuilder(val ob: ObjBase, val map: HashMap[String, Object]) extends StateMap(ob) {
	def this(ob: ObjBase) = this(ob, new HashMap[String, Object])
	
	def toImmutable: RobotState = new RobotState(ob, map.toMap)
}
