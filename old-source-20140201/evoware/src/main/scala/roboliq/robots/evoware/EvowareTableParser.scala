package roboliq.robots.evoware

import grizzled.slf4j.Logger
import roboliq.core._


case class HotelObject(
	parent: Carrier,
	n: Int // Value of unknown significance
)

case class ExternalObject(
	n1: Int, // Value of unknown significance
	n2: Int, // Value of unknown significance
	carrier: Carrier
)

/**
 * Parses an Evoware `.esc` script file, extracting the table setup.
 */
object EvowareTableParser {
	import EvowareFormat._

	private val logger = Logger("roboliq.robots.evoware.EvowareTableParser")
	
	def parseFile(carrierData: EvowareCarrierData, sFilename: String): EvowareTableData = {
		val lsLine = scala.io.Source.fromFile(sFilename, "ISO-8859-1").getLines.toList.drop(7)
		//println(lsLine.takeWhile(_ != "--{ RPG }--").length)
		val (_, l) = EvowareFormat.splitSemicolons(lsLine(1))
		val (tableFile, rest) = parse14(carrierData, l, lsLine.drop(2))
		//println("parseFile: "+rest.takeWhile(_ != "--{ RPG }--"))
		tableFile
	}

	def parse14(configFile: EvowareCarrierData, l: List[String], lsLine: List[String]): Tuple2[EvowareTableData, List[String]] = {
		import configFile._
		val lCarrier_? = parse14_getCarriers(mapIdToCarrier, l.init)
		val (lTableInfo, lsLine2) = parse14_getLabwareObjects(mapNameToLabwareModel, lCarrier_?, lsLine, Nil)
		val (lHotelObject, lsLine3) = parse14_getHotelObjects(mapIdToCarrier, lsLine2)
		val (lExternalObject, lsLine4) = parse14_getExternalObjects(mapNameToCarrier, lsLine3)
		val (mapSiteToExternalLabwareModel, lsLine5) = parse14_getExternalLabwares(mapIdToCarrier, mapNameToLabwareModel, lsLine4)
		val (mapCarrierToGrid2, lsLine6) = parse14_getExternalCarrierGrids(lExternalObject, lsLine5)
		
		val mapSiteToLabel = lTableInfo.map(o => o._1 -> o._2).toMap
		val mapSiteToLabwareModel = (
			lTableInfo.map(o => o._1 -> o._3) ++
			mapSiteToExternalLabwareModel
		).toMap
		val mapCarrierToGrid1 = lCarrier_?.zipWithIndex.collect({ case (Some(o), iGrid) => o -> iGrid }).toMap
		val mapCarrierToGrid = mapCarrierToGrid1 ++ mapCarrierToGrid2
		
		val tableFile = new EvowareTableData(
			configFile,
			lCarrier_?,
			lHotelObject,
			lExternalObject,
			mapCarrierToGrid,
			mapSiteToLabel,
			mapSiteToLabwareModel
		)
		
		logger.trace("tablefile:")
		logger.trace(tableFile.toDebugString)
		(tableFile, lsLine6)
	}
	
	def parse14_getCarriers(
		mapIdToCarrier: Map[Int, Carrier],
		l: List[String]
	): List[Option[Carrier]] = {
		l.map(s => {
			val id = s.toInt
			if (id == -1) None
			else mapIdToCarrier.get(id)
		}).toList
		/*
		for ((item, iGrid) <- l.zipWithIndex) {
			if (item != "-1") {
				val id = item.toInt
				println(iGrid + ": " + map(id))
			}
		}*/
	}
	
	def parse14_getLabwareObjects(
		mapNameToLabwareModel: Map[String, LabwareModel],
		//iGrid: Int,
		lCarrier_? : List[Option[Carrier]],
		lsLine: List[String],
		acc: List[Tuple3[CarrierSite, String, LabwareModel]]
	): Tuple2[List[Tuple3[CarrierSite, String, LabwareModel]], List[String]] = {
		lCarrier_? match {
			case Nil => (acc, lsLine)
			case None :: rest => parse14_getLabwareObjects(mapNameToLabwareModel, rest, lsLine.tail, acc)
			case Some(carrier) :: rest =>
				val (n0, l0) = EvowareFormat.splitSemicolons(lsLine(0))
				val (n1, l1) = EvowareFormat.splitSemicolons(lsLine(1))
				// FIME: for debug only
				if (!(n0 == 998 && n1 == 998 && l0(0).toInt == carrier.nSites)) {
					logger.error("ERROR: parse14_getLabwareObjects:")
					logger.error(carrier)
					logger.error(lsLine.head)
					logger.error((n0, l0, n1, l1, carrier.nSites))
				}
				// ENDFIX
				assert(n0 == 998 && n1 == 998 && l0(0).toInt == carrier.nSites)
				//println(iGrid+": "+carrier)
				val l = (for (iSite <- 0 until carrier.nSites) yield {
					//println("\t"+i+": "+l0(i+1)+", "+l1(i))
					val sName = l0(iSite+1)
					if (sName.isEmpty()) None
					else Some(CarrierSite(carrier, iSite), l1(iSite), mapNameToLabwareModel(sName))
				}).toList.flatten
				parse14_getLabwareObjects(mapNameToLabwareModel, rest, lsLine.drop(2), acc ++ l)
		}
	}
	
	def parse14_getHotelObjects(
		mapIdToCarrier: Map[Int, Carrier],
		lsLine: List[String]
	): Tuple2[List[HotelObject], List[String]] = {
		val (n0, l0) = EvowareFormat.splitSemicolons(lsLine(0))
		assert(n0 == 998)
		val nHotels = l0(0).toInt
		val lHotelObject = lsLine.tail.take(nHotels).map(s => {
			val (n, l) = EvowareFormat.splitSemicolons(s)
			assert(n == 998)
			val id = l(0).toInt
			val iGrid = l(1).toInt
			val parent = mapIdToCarrier(id)
			HotelObject(parent, iGrid)
		})
		(lHotelObject, lsLine.drop(1 + nHotels))
	}
	
	def parse14_getExternalObjects(
		mapNameToCarrier: Map[String, Carrier],
		lsLine: List[String]
	): Tuple2[List[ExternalObject], List[String]] = {
		val (n0, l0) = EvowareFormat.splitSemicolons(lsLine(0))
		assert(n0 == 998)
		val nObjects = l0(0).toInt
		val lObject = lsLine.tail.take(nObjects).map(s => {
			val (n, l) = EvowareFormat.splitSemicolons(s)
			assert(n == 998)
			val n1 = l(0).toInt
			val n2 = l(1).toInt
			val sName = l(2)
			val carrier =
				if (n1 == 0) mapNameToCarrier(sName)
				else new Carrier(sName, -1, 1)
			ExternalObject(n1, n2, carrier)
		})
		(lObject, lsLine.drop(1 + nObjects))
	}
	
	def parse14_getExternalLabwares(
		mapIdToCarrier: Map[Int, Carrier],
		mapNameToLabwareModel: Map[String, LabwareModel],
		lsLine: List[String]
	): Tuple2[Map[CarrierSite, LabwareModel], List[String]] = {
		val (n0, l0) = EvowareFormat.splitSemicolons(lsLine(0))
		assert(n0 == 998)
		val nObjects = l0(0).toInt
		val mapSiteToLabwareModel = lsLine.tail.take(nObjects).map(s => {
			val (n, l) = EvowareFormat.splitSemicolons(s)
			assert(n == 998)
			val idCarrier = l(0).toInt
			val sName = l(1)
			val carrier = mapIdToCarrier(idCarrier)
			val labwareModel = mapNameToLabwareModel(sName)
			CarrierSite(carrier, 0) -> labwareModel
		}).toMap
		(mapSiteToLabwareModel, lsLine.drop(1 + nObjects))
	}
	
	def parse14_getExternalCarrierGrids(
		lExternalObject: List[ExternalObject],
		lsLine: List[String]
	): Tuple2[Map[Carrier, Int], List[String]] = {
		val map = (lExternalObject zip lsLine).map(pair => {
			val (external, sLine) = pair
			val (n, l) = EvowareFormat.splitSemicolons(sLine)
			assert(n == 998)
			val iGrid = l(0).toInt
			external.carrier -> iGrid
		}).toMap
		(map, lsLine.drop(lExternalObject.length))
	}
}
