package roboliq.common

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap


class Reagent extends Obj { thisObj =>
	type Config = ReagentConfig
	type State = ReagentState
	
	var sName_? : Option[String] = None
	var sFamily_? : Option[String] = None
	var contaminants = Set[Contaminant.Value]()
	var group_? : Option[LiquidGroup] = None
	var multipipetteThreshold_? : Option[Double] = None
	
	override def getLabel(kb: KnowledgeBase): String = {
		sName_?.getOrElse(toString)
	}

	def createConfigAndState0(): Result[Tuple2[Config, State]] = {
		val setup = this
		val errors = new ArrayBuffer[String]

		if (setup.sName_?.isEmpty)
			errors += "name not set"
		if (setup.sFamily_?.isEmpty)
			errors += "family not set"
				
		if (!errors.isEmpty)
			return Error(errors)

		val liquid = new Liquid(
				setup.sName_?.get,
				setup.sFamily_?.get,
				setup.contaminants,
				setup.group_?.getOrElse(new LiquidGroup()),
				setup.multipipetteThreshold_?.getOrElse(0.0))
			
		val conf = new ReagentConfig(
				obj = this,
				liquid = liquid)
		val state = new ReagentState(
				conf = conf)
		
		Success(conf, state)
	}

	class StateWriter(map: HashMap[Obj, ObjState]) {
		def state = map(thisObj).asInstanceOf[State]
	}
	//def stateWriter(map: HashMap[ThisObj, State]) = new StateWriter(this, map)
	def stateWriter(builder: StateBuilder): StateWriter = new StateWriter(builder.map)
}

class ReagentConfig(
	val obj: Reagent,
	val liquid: Liquid
) extends ObjConfig {
	override def toString = liquid.toString
}

class ReagentState(val conf: ReagentConfig) extends ObjState
