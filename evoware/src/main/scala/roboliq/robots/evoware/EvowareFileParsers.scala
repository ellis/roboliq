package roboliq.robots.evoware


sealed abstract class EvowareModel

case class Carrier(
	val sName: String,
	val id: Int,
	val nSites: Int
) extends EvowareModel

case class LabwareModel(
	val sName: String,
	val nRows: Int,
	val nCols: Int
) extends EvowareModel

case class Vector(
	val idCarrier: Int,
	val sClass: String, // Wide, Narrow, or user-defined
	val iRoma: Int
) extends EvowareModel

case class CarrierSite(
	carrier: Carrier,
	iSite: Int
)

case class LabwareObject(
	site: CarrierSite,
	labwareModel: LabwareModel,
	sLabel: String
)

case class HotelObject(
	parent: Carrier,
	n: Int // Value of unknown significance
)

case class ExternalObject(
	n1: Int, // Value of unknown significance
	n2: Int, // Value of unknown significance
	carrier: Carrier
)

object EvowareFormat {
	def splitSemicolons(sLine: String): Tuple2[Int, Array[String]] = {
		val l = sLine.split(";", -1)//.init
		val sLineKind = l.head
		val nLineKind = sLineKind.toInt
		(nLineKind, l.tail)
	}
}

object CarrierParser {
	import EvowareFormat._
	
	def loadCarrierConfig(sFilename: String): List[EvowareModel] = {
		val lsLine = scala.io.Source.fromFile(sFilename, "ISO-8859-1").getLines.toList
		//val lsLine = sInput.split("\r?\n", -1).toList
		def step(lsLine: List[String], acc: List[EvowareModel]): List[EvowareModel] = {
			if (lsLine.isEmpty)
				return acc
			parseModel(lsLine) match {
				case (None, lsLine2) => step(lsLine2, acc)
				case (Some(model), lsLine2) =>
					//println(sec)
					step(lsLine2, model::acc)
			}
		}
		step(lsLine.drop(4), Nil)
		//val ls14 = sLine14.split(";", -1).tail.init.toList
	}
	
	def parseModel(lsLine: List[String]): Tuple2[Option[EvowareModel], List[String]] = {
		val sLine0 = lsLine.head
		val (nLineKind, l) = splitSemicolons(sLine0)
		nLineKind match {
			case 13 => parse13(l, lsLine.tail)
			case 15 => parse15(l, lsLine.tail)
			case 17 => parse17(l, lsLine.tail)
			case _ => (None, lsLine.tail)
		}
	}
	
	def parse13(l: Array[String], lsLine: List[String]): Tuple2[Option[Carrier], List[String]] = {
		val sName = l.head
		val l1 = l(1).split("/")
		val sId = l1(0)
		//val sBarcode = l1(1)
		val id = sId.toInt
		val nSites = l(4).toInt
		(Some(Carrier(sName, id, nSites)), lsLine.drop(6 + nSites))
	}
	
	def parse15(l: Array[String], lsLine: List[String]): Tuple2[Option[LabwareModel], List[String]] = {
		val sName = l.head
		val ls2 = l(2).split("/")
		val nCols = ls2(0).toInt
		val nRows = ls2(1).toInt
		//val nCompartments = ls2(2).toInt
		val nArea = l(5).toDouble // mm^2
		//val nTipsPerWell = l(6).toDouble
		//val nDepth = l(15).toDouble // mm
		val nWellLines = l(20).toInt
		// shape: flat, round, v-shaped (if nDepth == 0, then flat, if > 0 then v-shaped, if < 0 then round
		// labware can have lid
		
		(Some(LabwareModel(sName, nRows, nCols)), lsLine.drop(10 + nWellLines))
	}
	
	def parse17(l: Array[String], lsLine: List[String]): Tuple2[Option[Vector], List[String]] = {
		println(l.toList)
		val l0 = l.head.split("_")
		if (l0.length < 3)
			return (None, lsLine)
		
		val sClass = l0(1)
		val iRoma = l0(2).toInt - 1
		val nSteps = l(3).toInt
		val idCarrier = l(4).toInt
		(Some(Vector(idCarrier, sClass, iRoma)), lsLine.drop(nSteps))
	}
}

class EvowareConfigFile(sCarrierCfg: String) {
	val models = CarrierParser.loadCarrierConfig(sCarrierCfg)
	val mapIdToCarrier = models.collect({case o: Carrier => o.id -> o}).toMap
	val mapNameToCarrier = models.collect({case o: Carrier => o.sName -> o}).toMap
	val mapNameToLabwareModel = models.collect({case o: LabwareModel => o.sName -> o}).toMap
	val mapCarrierToVectors = models.collect({case o: Vector if mapIdToCarrier.contains(o.idCarrier) => mapIdToCarrier(o.idCarrier) -> o})
		.groupBy(_._1)
		.map(pair => pair._1 -> pair._2.map(_._2))
		.toMap
}

class EvowareTableFile(
	val configFile: EvowareConfigFile,
	val lLabwareObject: List[LabwareObject],
	val lHotelObject: List[HotelObject],
	val lExternalObject: List[ExternalObject],
	val lExternalLabwareObject: List[LabwareObject],
	val mapCarrierToGrid: Map[Carrier, Int]
) {
	def print() {
		lLabwareObject.foreach(println)
		lHotelObject.foreach(println)
		lExternalObject.foreach(println)
		lExternalLabwareObject.foreach(println)
		mapCarrierToGrid.toList.sortBy(_._2).foreach(println)
	}
}

object EvowareTableParser {
	import EvowareFormat._

	def parseFile(configFile: EvowareConfigFile, sFilename: String): EvowareTableFile = {
		val lsLine = scala.io.Source.fromFile(sFilename, "ISO-8859-1").getLines.toList.drop(7)
		//println(lsLine.takeWhile(_ != "--{ RPG }--").length)
		val (_, l) = EvowareFormat.splitSemicolons(lsLine(1))
		val (tableFile, rest) = parse14(configFile, l, lsLine.drop(2))
		println(rest.takeWhile(_ != "--{ RPG }--"))
		tableFile
	}

	def parse14(configFile: EvowareConfigFile, l: Array[String], lsLine: List[String]): Tuple2[EvowareTableFile, List[String]] = {
		import configFile._
		val lCarrier_? = parse14_getCarriers(mapIdToCarrier, l.init)
		val (lLabwareObject, lsLine2) = parse14_getLabwareObjects(mapNameToLabwareModel, lCarrier_?, lsLine, Nil)
		val (lHotelObject, lsLine3) = parse14_getHotelObjects(mapIdToCarrier, lsLine2)
		val (lExternalObject, lsLine4) = parse14_getExternalObjects(mapNameToCarrier, lsLine3)
		val (lExternalLabwareObject, lsLine5) = parse14_getExternalLabwares(mapIdToCarrier, mapNameToLabwareModel, lsLine4)
		val (mapCarrierToGrid2, lsLine6) = parse14_getExternalCarrierGrids(lExternalObject, lsLine5)
		
		val mapCarrierToGrid1 = lCarrier_?.zipWithIndex.collect({ case (Some(o), iGrid) => o -> iGrid }).toMap
		val mapCarrierToGrid = mapCarrierToGrid1 ++ mapCarrierToGrid2
		
		val tableFile = new EvowareTableFile(
			configFile,
			lLabwareObject,
			lHotelObject,
			lExternalObject,
			lExternalLabwareObject,
			mapCarrierToGrid
		)
		(tableFile, lsLine6)
	}
	
	def parse14_getCarriers(
		mapIdToCarrier: Map[Int, Carrier],
		l: Array[String]
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
		acc: List[LabwareObject]
	): Tuple2[List[LabwareObject], List[String]] = {
		lCarrier_? match {
			case Nil => (acc, lsLine)
			case None :: rest => parse14_getLabwareObjects(mapNameToLabwareModel, rest, lsLine.tail, acc)
			case Some(carrier) :: rest =>
				val (n0, l0) = EvowareFormat.splitSemicolons(lsLine(0))
				val (n1, l1) = EvowareFormat.splitSemicolons(lsLine(1))
				assert(n0 == 998 && n1 == 998 && l0(0).toInt == carrier.nSites)
				//println(iGrid+": "+carrier)
				val l = (for (iSite <- 0 until carrier.nSites) yield {
					//println("\t"+i+": "+l0(i+1)+", "+l1(i))
					val sName = l0(iSite+1)
					if (sName.isEmpty()) None
					else Some(LabwareObject(CarrierSite(carrier, iSite), mapNameToLabwareModel(sName), l1(iSite)))
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
	): Tuple2[List[LabwareObject], List[String]] = {
		val (n0, l0) = EvowareFormat.splitSemicolons(lsLine(0))
		assert(n0 == 998)
		val nObjects = l0(0).toInt
		val lObject = lsLine.tail.take(nObjects).tail.map(s => {
			val (n, l) = EvowareFormat.splitSemicolons(s)
			assert(n == 998)
			val idCarrier = l(0).toInt
			val sName = l(1)
			val carrier = mapIdToCarrier(idCarrier)
			val labwareModel = mapNameToLabwareModel(sName)
			LabwareObject(CarrierSite(carrier, 0), labwareModel, "")
		})
		(lObject, lsLine.drop(1 + nObjects))
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

/*
object T {
	def test() {
		val configFile = new EvowareConfigFile("/home/ellisw/tmp/tecan/carrier.cfg")
		configFile.mapCarrierToVectors.foreach(println)
		//models.foreach(println)
		val tableFile = EvowareTableParser.parseFile(configFile, "/home/ellisw/src/roboliq/ellis_pcr1_corrected.esc")
		tableFile.print()
	}
}
*/
