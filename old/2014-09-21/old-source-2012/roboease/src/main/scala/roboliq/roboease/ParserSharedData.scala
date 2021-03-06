package roboliq.roboease

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.Stack

import roboliq.common._
import roboliq.commands.pipette.PipettePolicy
import roboliq.commands.pipette.TipModel


/** @param mapLcToPolicy Map liquid class name to PipettePolicy */
class ParserSharedData(
	val dirProc: java.io.File,
	val dirLog: java.io.File,
	val mapTipModel: Map[String, TipModel],
	val mapLcToPolicy: Map[String, PipettePolicy],
	val mapPlateModel: Map[String, PlateModel]
) {
	val kb = new KnowledgeBase
	var file: java.io.File = null
	var sTable: String = null
	var sHeader: String = null
	val mapRacks = new HashMap[String, Rack]
	val mapReagents = new HashMap[String, Reagent]
	val mapVars = new HashMap[String, String]
	val mapLists = new HashMap[String, List[String]]
	val mapOptions = new HashMap[String, String]
	val mapRackToPlate = new HashMap[Rack, PlateObj]
	val mapLabware = new HashMap[Tuple2[Int, Int], Labware]
	val mapMixDefs = new HashMap[String, MixDef]
	/** When an external procedure is called, we substitute certain names for others */
	var mapSubstitutions: Map[String, String] = Map()
	
	val stackFile = new Stack[java.io.File]()
	///** When a script is called, the parent's mapVars is pushed onto this stack, then popped again when the called script is finished */
	//val stackVarsFromParent = new Stack[HashMap[String, String]]
	//val stackListsFromParent = new Stack[HashMap[String, List[String]]]
	
	/*
	var iLineCurrent: Int = 0
	var sLineCurrent: String = null
	
	private val m_errors = new ArrayBuffer[LineError]
	def addError(sError: String) {
		m_errors += LineError(iLineCurrent, None, sLineCurrent, sError)
	}
	def errors = m_errors.toSeq
	*/
	
	def subst(id: String): String = mapSubstitutions.getOrElse(id, id)
	
	def getList(id: String) = Result.get(mapLists.get(subst(id)), "unknown list \""+id+"\"")
	def getMixDef(id: String) = Result.get(mapMixDefs.get(subst(id)), "unknown mix definition \""+id+"\"")
	def getPipettePolicy(id: String) = Result.get(mapLcToPolicy.get(subst(id)), "unknown liquid class \""+id+"\"")
	def getRack(id: String) = Result.get(mapRacks.get(subst(id)), "unknown rack \""+id+"\"")
	def getReagent(id: String) = Result.get(mapReagents.get(subst(id)), "unknown reagent \""+id+"\"; known reagents are "+mapReagents.keys.toList.sortBy(identity).mkString(", "))
	
	def getLiquidClass(id: String): Result[String] = {
		for { _ <- Result.assert(mapLcToPolicy.contains(subst(id)), "unknown liquid class \""+id+"\"") }
		yield { id }
	}

	def getDim(plate: PlateObj): Result[PlateSetupDimensionL4] = {
		Result.get(plate.dim_?, "Plate \""+plate+"\" requires dimensions")
	}
}
