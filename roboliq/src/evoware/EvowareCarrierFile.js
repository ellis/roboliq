import _ from 'lodash';
import fs from 'fs';
import sprintf from 'sprintf';


/**
 * Tuple for location refered to by carrier+grid+site indexes
 * @typedef {object} CarrierGridSiteIndex
 * @property {integer} carrierId - ID for the carrier
 * @property {integer} gridIndex - 1-based index of grid
 * @property {integer} siteIndex -  0-based index of site
 */

/**
 * Tuple for location refered to by carrier+site index
 * @typedef {object} CarrierSiteIndex
 * @property {integer} carrierId - ID for the carrier
 * @property {integer} siteIndex -  0-based index of site
 */

/**
 * A base type for evoware models, one of Carrier, EvowareLabwareModel, or Vector.
 * @typedef {object} EvowareModel
 * @property {string} type - the type of model
 */

/**
 * A Carrier object
 * @typedef {EvowareModel} Carrier
 * @property {string} type - should be "Carrier"
 * @property {string} name
 * @property {integer} id
 * @property {integer} siteCount
 * @property {string} [deviceName]
 * @property {string} [partNo]
 */

/**
 * An evoware labware model
 * @typedef {EvowareModel} EvowareLabwareModel
 * @property {string} type - should be "EvowareLabwareModel"
 * @property {string} name
 * @property {integer} rows
 * @property {integer} cols
 * @property {number} ul - maximum volume of wells
 * @property {array} sites - list of CarrierSiteIndexes where this labware can be placed.
 */

/**
 * A tranporter "vector", related to movements that the RoMas can make
 * @typedef {EvowareModel} Vector
 * @property {string} type - should be "Vector"
 * @property {integer} carrierId - which carrier this vector is for
 * @property {string} class - Wide, Narrow, or user-defined
 * @property {integer} romaId - which RoMa this vector is for
 */

/**
 * An object representing an evoware carrier file
 *
 * @typedef {object} EvowareCarrierData
 * @property {array} models - array of the evoware models
 * @property {object} idToCarrier - map of carrier ID to Carrier object
 * @property {object} nameToCarrier - map of carrier name to Carrier object
 * @property {object} nameToLabwareModel - map of name to EvowareLabwareModel
 * @property {carrierIdToVectors} - map of carrier ID to list of Vectors
 */


/*
case class CarrierSite(
	carrier: Carrier,
	iSite: Int
)
*/

/**
 * Split an evoware carrier line into its components.
 * The first component is an integer that identifies the type of line.
 * The remaining components are returned as a list of string.
 *
 * @param  {string} line - a text line from Evoware's carrier file
 * @return {array} Returns a pair [kind, items], where kind is an integer
 *   identifying the type of line, and items is a string array of the remaining
 *   components of the line.
 */
function splitSemicolons(line) {
	const l = line.split(";");
	const kind = parseInt(l[0]);
	return [kind, _.tail(l)];
}

/**
 * Print debug output: carrier id, carrier name.
 * @param {EvowareCarrierData} evowareCarrierData
 */
export function printCarriersById(evowareCarrierData) {
	const l0 = _.keys(evowareCarrierData.idToCarrier);
	// Sort by carrier ID
	const l1 = _.sortBy(l0, x => parseInt(x[0]));
	l1.forEach(pair => console.log(sprintf("%03d %s", pair[0], pair[1].name)));
}

/**
 * Create an EvowareCarrierData object from an array of evoware models.
 * @param  {array} models - array of evoware models
 * @return {EvowareCarrierData}
 */
function makeEvowareCarrierData(models) {
	const idToCarrier = _(models).filter(x => x.type === "Carrier").map(x => [x.id, x]).zipObject().value();
	return {
		models,
		idToCarrier,
		nameToCarrier: _(models).filter(x => x.type === "Carrier").map(x => [x.name, x]).zipObject().value(),
		nameToLabwareModel: _(models).filter(x => x.type === "EvowareLabwareModel").map(x => [x.name, x]).zipObject().value(),
		carrierIdToVectors: _(models).filter(x => x.type === "Vector").groupBy('carrierId').value()
	)
}

/**
 * Load an evoware carrier file and return its model data.
 * @param  {string} filename - path to the carrier file
 * @return {EvowareCarrierData}
 */
export function loadEvowareCarrierData(filename) {
	const models = loadEvowareModels(filename);
	return makeEvowareCarrierData(models);
}

/**
 * Parses the file `Carrier.cfg` into a list of `EvowareModels`.
 * @param {string} filename - path to the carrier file
 * @return {array} an array of EvowareModels (e.g. Carriers, Vectors, EvowareLabwareModels)
 */
function loadEvowareModels(filename) {
	CONTINUE
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
