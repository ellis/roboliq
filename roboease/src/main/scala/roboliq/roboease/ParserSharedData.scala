package roboliq.roboease

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.pipette.PipettePolicy
import roboliq.commands.pipette.TipModel


class ParserSharedData(
	val mapTipModel: Map[String, TipModel],
	/** Map liquid class name to PipettePolicy */
	val mapLcToPolicy: Map[String, PipettePolicy],
	val mapPlateModel: Map[String, PlateModel]
) {
	val kb = new KnowledgeBase
	var sTable: String = null
	var sHeader: String = null
	val mapRacks = new HashMap[String, Rack]
	//val lReagentsInWells = new ArrayBuffer[Tuple2[Reagent, Seq[Well]]]
	//val mapLiquids = new HashMap[String, Liquid]
	val mapReagents = new HashMap[String, Reagent]
	//val mapReagentToPolicy = new HashMap[Reagent, PipettePolicy]
	val mapVars = new HashMap[String, String]
	val mapLists = new HashMap[String, List[String]]
	val mapOptions = new HashMap[String, String]
	//val mapLocToPlate = new HashMap[String, Plate]
	val mapRackToPlate = new HashMap[Rack, Plate]
	val mapLabware = new HashMap[Tuple2[Int, Int], Labware]
	val mapMixDefs = new HashMap[String, MixDef]
	//val mapLabware = new HashMap[String, Tuple2[String, String]]
	
	var iLineCurrent: Int = 0
	var sLineCurrent: String = null
	
	private val m_errors = new ArrayBuffer[LineError]
	def addError(sError: String) {
		m_errors += LineError(iLineCurrent, None, sLineCurrent, sError)
	}
	def errors = m_errors.toSeq
	
	
	def getList(id: String) = Result.get(mapLists.get(id), "unknown list \""+id+"\"")
	def getMixDef(id: String) = Result.get(mapMixDefs.get(id), "unknown mix definition \""+id+"\"")
	def getPipettePolicy(id: String) = Result.get(mapLcToPolicy.get(id), "unknown liquid class \""+id+"\"")
	def getRack(id: String) = Result.get(mapRacks.get(id), "unknown rack \""+id+"\"")
	def getReagent(id: String) = Result.get(mapReagents.get(id), "unknown reagent \""+id+"\"")
	
	def getLiquidClass(id: String): Result[String] = {
		for { _ <- Result.assert(mapLcToPolicy.contains(id), "unknown liquid class \""+id+"\"") }
		yield { id }
	}

	def getDim(plate: Plate): Result[PlateSetupDimensionL4] = {
		val setup = kb.getPlateSetup(plate)
		Result.get(setup.dim_?, "Plate \""+plate+"\" requires dimensions")
	}
}
