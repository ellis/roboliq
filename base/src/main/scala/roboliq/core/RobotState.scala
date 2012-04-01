package roboliq.core

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet


trait StateMap {
	val ob: ObjBase
	val map: collection.Map[Object, Object]
	def apply(o: Object) = map(o)
	
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
}

class RobotState(val ob: ObjBase, val map: Map[Object, Object]) extends StateMap {
	def filterByValueType[State <: Object](implicit m: Manifest[State]): Map[Object, State] = {
		map.filter(pair => m.erasure.isInstance(pair._2)).mapValues(_.asInstanceOf[State])
	}
}

class StateBuilder(val ob: ObjBase, val map: HashMap[Object, Object]) extends StateMap {
	def this(ob: ObjBase, states: RobotState) = this(ob, HashMap[Object, Object](states.map.toSeq : _*))
	def this(ob: ObjBase) = this(ob, new HashMap[Object, Object])
	
	def toImmutable: RobotState = new RobotState(ob, map.toMap)
}
