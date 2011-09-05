package roboliq.roboease

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._


class ParserSharedData {
	val kb = new KnowledgeBase
	var sHeader: String = null
	val mapRacks = new HashMap[String, Rack]
	val mapLiquids = new HashMap[String, Liquid]
	val mapDefaultLiquidClass = new HashMap[Liquid, String]
	val mapVars = new HashMap[String, String]
	val mapLists = new HashMap[String, List[String]]
	val mapOptions = new HashMap[String, String]
	val mapLocToPlate = new HashMap[String, Plate]
	val mapLabware = new HashMap[Tuple2[Int, Int], String]
	//val mapLabware = new HashMap[String, Tuple2[String, String]]
	
	var iLineCurrent: Int = 0
	var sLineCurrent: String = null
	
	private val m_errors = new ArrayBuffer[LineError]
	def addError(sError: String) {
		m_errors += LineError(iLineCurrent, None, sLineCurrent, sError)
	}
	def errors = m_errors.toSeq
}
