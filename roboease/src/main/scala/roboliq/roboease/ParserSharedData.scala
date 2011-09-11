package roboliq.roboease

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.pipette.PipettePolicy
import roboliq.commands.pipette.TipModel


class ParserSharedData(
	val mapTipModel: Map[String, TipModel],
	/** Map liquid class name to PipettePolicy */
	val mapLcToPolicy: Map[String, PipettePolicy]
) {
	val kb = new KnowledgeBase
	var sHeader: String = null
	val mapRacks = new HashMap[String, Rack]
	//val lReagentsInWells = new ArrayBuffer[Tuple2[Reagent, Seq[Well]]]
	//val mapLiquids = new HashMap[String, Liquid]
	val mapReagents = new HashMap[String, Reagent]
	/** Default pipette policy for reagent */
	val mapReagentToPolicy = new HashMap[Reagent, PipettePolicy]
	val mapVars = new HashMap[String, String]
	val mapLists = new HashMap[String, List[String]]
	val mapOptions = new HashMap[String, String]
	val mapLocToPlate = new HashMap[String, Plate]
	val mapLabware = new HashMap[Tuple2[Int, Int], Labware]
	//val mapLabware = new HashMap[String, Tuple2[String, String]]
	
	var iLineCurrent: Int = 0
	var sLineCurrent: String = null
	
	private val m_errors = new ArrayBuffer[LineError]
	def addError(sError: String) {
		m_errors += LineError(iLineCurrent, None, sLineCurrent, sError)
	}
	def errors = m_errors.toSeq
}
