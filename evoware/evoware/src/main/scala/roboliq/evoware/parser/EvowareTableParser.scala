package roboliq.evoware.parser

import grizzled.slf4j.Logger


case class HotelObject(
	parent: Carrier,
	gridIndex: Int
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

	private def parse14(
		configFile: EvowareCarrierData,
		l: List[String],
		lsLine: List[String]
	): (EvowareTableData, List[String]) = {
		import configFile._
		val carrierId_l = parse14_getCarriers(l.init)
		val lCarrier_? : List[Option[Carrier]] = carrierId_l.map(carrierId_? => carrierId_?.flatMap(carrierId => configFile.mapIdToCarrier.get(carrierId)))
		val gridToInternalCarrierId_m: Map[Int, Int]
			= carrierId_l.zipWithIndex.collect({case (Some(carrierId), gridIndex) => gridIndex -> carrierId}).toMap
		val (lTableInfo, lsLine2) = parse14_getLabwareObjects(mapNameToLabwareModel, 0, lCarrier_?, lsLine, Nil)
		val (lHotelObject, lsLine3) = parse14_getHotelObjects(mapIdToCarrier, lsLine2)
		val (lExternalObject, lsLine4) = parse14_getExternalObjects(mapNameToCarrier, lsLine3)
		val (mapSiteToExternalLabwareModel, lsLine5) = parse14_getExternalLabwares(mapIdToCarrier, mapNameToLabwareModel, lsLine4)
		val (mapCarrierToGrid2, lsLine6) = parse14_getExternalCarrierGrids(lExternalObject, lsLine5)
		
		val mapSiteToLabel = lTableInfo.map(o => o._1 -> o._2).toMap
		val siteIdExternalToLabwareModel_l: List[(CarrierGridSiteIndex, EvowareLabwareModel)] = (
			mapSiteToExternalLabwareModel.toList.flatMap { case (CarrierSite(carrier, siteIndex), model) =>
				mapCarrierToGrid2.get(carrier).map(gridIndex => CarrierGridSiteIndex(carrier.id, gridIndex, siteIndex) -> model)
			}
		)
		val siteIdToLabwareModel_m: Map[CarrierGridSiteIndex, EvowareLabwareModel] = (
			lTableInfo.map(o => o._1 -> o._3) ++
			siteIdExternalToLabwareModel_l
		).toMap
		val mapCarrierToGrid1 = lCarrier_?.zipWithIndex.collect({ case (Some(o), iGrid) => o -> iGrid }).toMap
		val mapCarrierToGrid = mapCarrierToGrid1 ++ mapCarrierToGrid2
		
		val carrierIdToGrids_m: Map[Int, List[Int]] = {
			val l =
				gridToInternalCarrierId_m.toList.map(_.swap) ++
				mapCarrierToGrid2.toList.map({ case (carrier, gridIndex) => (carrier.id, gridIndex)})
			l.groupBy(_._1).mapValues(_.map(_._2))
		}
		
		val tableFile = new EvowareTableData(
			configFile,
			carrierId_l.toVector,
			lHotelObject,
			lExternalObject,
			carrierIdToGrids_m,
			//mapCarrierToGrid,
			mapSiteToLabel,
			siteIdToLabwareModel_m
		)
		
		logger.trace("tablefile:")
		logger.trace(tableFile.toDebugString)
		(tableFile, lsLine6)
	}
	
	private def parse14_getCarriers(
		l: List[String]
	): List[Option[Int]] = {
		l.map(s => {
			val id = s.toInt
			if (id == -1) None
			else Some(id)
		}).toList
	}
	
	def parse14_getLabwareObjects(
		mapNameToLabwareModel: Map[String, EvowareLabwareModel],
		gridIndex: Int,
		lCarrier_? : List[Option[Carrier]],
		lsLine: List[String],
		acc: List[Tuple3[CarrierGridSiteIndex, String, EvowareLabwareModel]]
	): Tuple2[List[Tuple3[CarrierGridSiteIndex, String, EvowareLabwareModel]], List[String]] = {
		lCarrier_? match {
			case Nil => (acc, lsLine)
			case None :: rest => parse14_getLabwareObjects(mapNameToLabwareModel, gridIndex + 1, rest, lsLine.tail, acc)
			case Some(carrier) :: rest =>
				val (n0, l0) = EvowareFormat.splitSemicolons(lsLine(0))
				val (n1, l1) = EvowareFormat.splitSemicolons(lsLine(1))
				/*// FIME: for debug only
				if (l0(0).isEmpty || !(n0 == 998 && n1 == 998 && l0(0).toInt == carrier.nSites)) {
					logger.error("ERROR: parse14_getLabwareObjects:")
					logger.error(carrier)
					logger.error(lsLine.head)
					logger.error((n0, l0, n1, l1, carrier.nSites))
				}
				// ENDFIX*/
				assert(n0 == 998 && n1 == 998 && l0(0).toInt == carrier.nSites)
				//println(iGrid+": "+carrier)
				val l = (for (iSite <- 0 until carrier.nSites) yield {
					//println("\t"+i+": "+l0(i+1)+", "+l1(i))
					val sName = l0(iSite+1)
					if (sName.isEmpty()) None
					else Some(CarrierGridSiteIndex(carrier.id, gridIndex, iSite), l1(iSite), mapNameToLabwareModel(sName))
				}).toList.flatten
				parse14_getLabwareObjects(mapNameToLabwareModel, gridIndex + 1, rest, lsLine.drop(2), acc ++ l)
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
				else new Carrier(sName, -1, 1, None, None)
			ExternalObject(n1, n2, carrier)
		})
		(lObject, lsLine.drop(1 + nObjects))
	}
	
	def parse14_getExternalLabwares(
		mapIdToCarrier: Map[Int, Carrier],
		mapNameToLabwareModel: Map[String, EvowareLabwareModel],
		lsLine: List[String]
	): (Map[CarrierSite, EvowareLabwareModel], List[String]) = {
		val (n0, l0) = EvowareFormat.splitSemicolons(lsLine(0))
		assert(n0 == 998)
		val nObjects = l0(0).toInt
		val mapSiteToLabwareModel = lsLine.tail.take(nObjects).map(s => {
			val (n, l) = EvowareFormat.splitSemicolons(s)
			assert(n == 998)
			val carrierId = l(0).toInt
			val sName = l(1)
			val carrier = mapIdToCarrier(carrierId)
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
			// We need to force the system liquid to be on grid -1
			val iGrid = if (external.carrier.id == -1) -1 else l(0).toInt
			external.carrier -> iGrid
		}).toMap
		(map, lsLine.drop(lExternalObject.length))
	}
}
