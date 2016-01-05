import _ from 'lodash';
import fs from 'fs';


/**
 * Tuple for location refered to by carrier+grid+site indexes
 * @typedef {object} CarrierGridSiteIndex
 * @property {integer} carrierId - ID for the carrier
 * @property {integer} gridIndex - 1-based index of grid
 * @property {integer} siteIndex -  0-based index of site
 */

//case class CarrierNameGridSiteIndex(carrierName: String, gridIndex: Int, siteIndex: Int)

/**
 * Tuple for location refered to by carrier+site index
 * @typedef {object} CarrierSiteIndex
 * @property {integer} carrierId - ID for the carrier
 * @property {integer} siteIndex -  0-based index of site
 */

 /**
  * A Carrier object
  * @typedef {object} Carrier
  * @property {string} name
  * @property {integer} id
  * @property {integer} siteCount
  * @property {string} [deviceName]
  * @property {string} [partNo]
  */


/**
 * @param sites list of (carrier ID, site index) where this labware can be placed.
 */
case class EvowareLabwareModel(
	val sName: String,
	val nRows: Int,
	val nCols: Int,
	val ul: Double,
	val sites: List[CarrierSiteIndex]
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
	def splitSemicolons(sLine: String): (Int, List[String]) = {
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
	val mapNameToLabwareModel: Map[String, EvowareLabwareModel],
	val mapCarrierToVectors: Map[Carrier, List[Vector]]
) {
	/**
	 * Print debug output: carrier id, carrier name
	 */
	def printCarriersById() {
		mapIdToCarrier.toList.sortBy(_._1).foreach(pair => println(f"${pair._1}%3d ${pair._2.sName}"))
	}
}

object EvowareCarrierData {
	def apply(models: List[EvowareModel]): EvowareCarrierData = {
		val mapIdToCarrier = models.collect({case o: Carrier => o.id -> o}).toMap
		new EvowareCarrierData(
			models = models,
			mapIdToCarrier = mapIdToCarrier,
			mapNameToCarrier = models.collect({case o: Carrier => o.sName -> o}).toMap,
			mapNameToLabwareModel = models.collect({case o: EvowareLabwareModel => o.sName -> o}).toMap,
			mapCarrierToVectors = models.collect({case o: Vector if mapIdToCarrier.contains(o.idCarrier) => mapIdToCarrier(o.idCarrier) -> o})
				.groupBy(_._1)
				.map(pair => pair._1 -> pair._2.map(_._2))
				.toMap
		)
	}

	def loadFile(filename: String): ResultC[EvowareCarrierData] = {
		try {
			val models = EvowareCarrierParser.loadFile(filename)
			ResultC.unit(apply(models))
		}
		catch {
			case ex: Throwable => ResultC.error(ex.getMessage)
		}
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
			// NOTE: There are also 23 and 25 lines, but I don't know what they're for.
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
		val deviceName_? = parse998(lsLine(nSites + 1)) match {
			case "" :: Nil => None
			case deviceName :: Nil => Some(deviceName)
			case _ => None
		}
		val partNo_? = parse998(lsLine(nSites + 3)) match {
			case "" :: Nil => None
			case x :: Nil => Some(x)
			case _ => None
		}
		/*
		if (sName == "Infinite M200") {
			println("parse13:")
			println(l)
			lsLine.take(nSites + 6).foreach(println)
			println(sName, sId, id, nSites, deviceName_?, partNo_?)
			println()
			println("lsLine(nSites + 1): " + parse998(lsLine(nSites + 1)))
			println("lsLine(nSites + 3): " + parse998(lsLine(nSites + 3)))
			println()
		}
		*/
		(Some(Carrier(sName, id, nSites, deviceName_?, partNo_?)), lsLine.drop(nSites + 6))
	}

	/**
	 * Parse a labware object; labware lines begin with "15"
	 */
	def parse15(l: List[String], lsLine: List[String]): Tuple2[Option[EvowareLabwareModel], List[String]] = {
		val sName = l.head
		val ls2 = l(2).split("/")
		val nCols = ls2(0).toInt
		val nRows = ls2(1).toInt
		//val nCompartments = ls2(2).toInt
		val ls4 = l(4).split("/")
		val zBottom = ls4(0).toInt
		val zDispense = ls4(2).toInt
		val nArea = l(5).toDouble // mm^2
		val nDepthOfBottom = l(15).toDouble // mm
		//val nTipsPerWell = l(6).toDouble
		//val nDepth = l(15).toDouble // mm
		val nCarriers = l(20).toInt
		// shape: flat, round, v-shaped (if nDepth == 0, then flat, if > 0 then v-shaped, if < 0 then round
		// labware can have lid

		// negative values for rounded bottom, positive for cone, 0 for flat
		val (nDepthOfCone, nDepthOfRound) =
			if (nDepthOfBottom > 0) (nDepthOfBottom, 0.0)
			else (0.0, -nDepthOfBottom)
		val r = math.sqrt(nArea / math.Pi)
		// Calculate the volume in microliters
		val ul = ((zBottom - zDispense) / 10.0 - nDepthOfCone - nDepthOfRound) * nArea +
			// Volume of a cone: (1/3)*area*height
			(nDepthOfCone * nArea / 3) +
			// Volume of a half-sphere:
			((4.0 / 6.0) * math.Pi * r * r *r)

		val lsCarrier = lsLine.take(nCarriers)
		val sites = lsCarrier.flatMap(s => {
			val ls = s.split(";").tail // split line, but drop the "998" prefix
			val idCarrier = ls(0).toInt
			val sitemask = ls(1)
			val (_, _, site_li) = Utils.parseEncodedIndexes(sitemask)
			site_li.map(site_i => CarrierSiteIndex(idCarrier, site_i))
		})

		(Some(EvowareLabwareModel(sName, nRows, nCols, ul, sites)), lsLine.drop(10 + nCarriers))
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
		((if (nSteps > 2) Some(Vector(idCarrier, sClass, iRoma)) else None), lsLine.drop(nSteps))
	}

	def parse998(s: String): List[String] = {
		EvowareFormat.splitSemicolons(s) match {
			case (n, l) => assert(n == 998); l.init
		}
	}
}
