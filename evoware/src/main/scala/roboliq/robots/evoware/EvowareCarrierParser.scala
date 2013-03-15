package roboliq.robots.evoware

import roboliq.core._


/** An item defined in the file `Carrier.cfg` */
sealed trait EvowareModel

case class Carrier(
	val sName: String,
	val id: Int,
	val nSites: Int
) extends EvowareModel

/**
 * @param sites list of (carrier ID, site index) where this labware can be placed.
 */
case class LabwareModel(
	val sName: String,
	val nRows: Int,
	val nCols: Int,
	val ul: Double,
	val sites: List[(Int, Int)]
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

object EvowareFormat {
	def splitSemicolons(sLine: String): Tuple2[Int, List[String]] = {
		val l = sLine.split(";", -1).toList//.init
		val sLineKind = l.head
		val nLineKind = sLineKind.toInt
		(nLineKind, l.tail)
	}
}

class EvowareCarrierData(
	val models: List[EvowareModel],
	val mapIdToCarrier: Map[Int, Carrier],
	val mapNameToCarrier: Map[String, Carrier],
	val mapNameToLabwareModel: Map[String, LabwareModel],
	val mapCarrierToVectors: Map[Carrier, List[Vector]]
)

object EvowareCarrierData {
	def apply(models: List[EvowareModel]): EvowareCarrierData = {
		val mapIdToCarrier = models.collect({case o: Carrier => o.id -> o}).toMap
		new EvowareCarrierData(
			models = models,
			mapIdToCarrier = mapIdToCarrier,
			mapNameToCarrier = models.collect({case o: Carrier => o.sName -> o}).toMap,
			mapNameToLabwareModel = models.collect({case o: LabwareModel => o.sName -> o}).toMap,
			mapCarrierToVectors = models.collect({case o: Vector if mapIdToCarrier.contains(o.idCarrier) => mapIdToCarrier(o.idCarrier) -> o})
				.groupBy(_._1)
				.map(pair => pair._1 -> pair._2.map(_._2))
				.toMap
		)
	}

	def loadFile(filename: String): EvowareCarrierData = {
		val models = EvowareCarrierParser.loadFile(filename)
		apply(models)
	}
}

/**
 * Parses the file `Carrier.cfg` into a list of `EvowareModels`.
 */
object EvowareCarrierParser {
	import EvowareFormat._
	
	def loadFile(sFilename: String): List[EvowareModel] = {
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
	
	/**
	 * Parse a carrier object; carrier lines begin with "13"
	 */
	def parse13(l: List[String], lsLine: List[String]): Tuple2[Option[Carrier], List[String]] = {
		val sName = l.head
		val l1 = l(1).split("/")
		val sId = l1(0)
		//val sBarcode = l1(1)
		val id = sId.toInt
		val nSites = l(4).toInt
		(Some(Carrier(sName, id, nSites)), lsLine.drop(6 + nSites))
	}
	
	/**
	 * Parse a labware object; labware lines begin with "15"
	 */
	def parse15(l: List[String], lsLine: List[String]): Tuple2[Option[LabwareModel], List[String]] = {
		val sName = l.head
		val ls2 = l(2).split("/")
		val nCols = ls2(0).toInt
		val nRows = ls2(1).toInt
		//val nCompartments = ls2(2).toInt
		val ls4 = l(4).split("/")
		val zBottom = ls4(0).toInt
		val zDispense = ls4(2).toInt
		val nArea = l(5).toDouble // mm^2
		val nDepthOfCone = l(15).toDouble // mm
		//val nTipsPerWell = l(6).toDouble
		//val nDepth = l(15).toDouble // mm
		val nCarriers = l(20).toInt
		// shape: flat, round, v-shaped (if nDepth == 0, then flat, if > 0 then v-shaped, if < 0 then round
		// labware can have lid
		
		// Calculate the volume in microliters
		val ul = ((zBottom - zDispense) / 10.0 - nDepthOfCone) * nArea +
			// Area of a cone: (1/3)*area*height
			(nDepthOfCone * nArea / 3)
		
		val lsCarrier = lsLine.take(nCarriers)
		val sites = lsCarrier.flatMap(s => {
			val ls = s.split(";").tail // split line, but drop the "998" prefix
			val idCarrier = ls(0).toInt
			val sitemask = ls(1)
			Utils.parseEncodedIndexes(sitemask) match {
				case RqSuccess((_, _, site_li), _) => site_li.map(idCarrier -> _)
				case _ => Nil
			}
		})
		
		(Some(LabwareModel(sName, nRows, nCols, ul, sites)), lsLine.drop(10 + nCarriers))
	}
	
	/**
	 * Parse a vector object; vector lines begin with "17"
	 */
	def parse17(l: List[String], lsLine: List[String]): Tuple2[Option[Vector], List[String]] = {
		//println("parse17: "+l.toList)
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
