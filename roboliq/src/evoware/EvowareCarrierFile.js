import _ from 'lodash';
import assert from 'assert';
import fs from 'fs';
import iconv from 'iconv-lite';
import lineByLine from 'n-readlines';
import sprintf from 'sprintf-js';


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

export class CarrierSiteIndex {
	constructor(carrierId, siteIndex) {
		this.carrierId = carrierId;
		this.siteIndex = siteIndex;
	}
}

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
export class Carrier {
	constructor(type, name, id, siteCount, deviceName, partNo) {
		this.type = type;
		this.name = name;
		this.id = id;
		this.siteCount = siteCount;
		this.deviceName = deviceName;
		this.partNo = partNo;
	}
}

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
 * @property {string} clazz - Wide, Narrow, or user-defined
 * @property {integer} romaId - which RoMa this vector is for
 */

export class Vector {
	constructor(type, carrierId, clazz, romaId) {
		this.type = type;
		this.carrierId = carrierId;
		this.clazz = clazz;
		this.romaId = romaId;
	}
}

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
	};
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
	const models = [];

	const raw = fs.readFileSync(filename);
	const filedata = iconv.decode(raw, "ISO-8859-1");
	const lines = filedata.split("\n");
	//console.log("lines:\n"+lines)
	//console.log(lines.length);
	let lineIndex = 4; // skip 4 lines

	// Find models in the carrier file
	while (lineIndex < lines.length) {
		const [lineIndex2, model] = parseModel(lines, lineIndex)
		console.log({model})
		if (!_.isUndefined(model))
			models.push(model)
		assert(lineIndex2 > lineIndex);
		console.log({lineIndex2})
		lineIndex = lineIndex2;
	}

	return models;
}

/**
 * Parse the line and return the next lineIndex and a model, if relevant.
 * @param {array} lines - array of lines from the Carrier.cfg
 * @param {number} lineIndex - the current line to inspect
 * @return {array} a pair [linesUsed, model], where linesUsed is the new lineIndex and model is an optional model.
 */
function parseModel(lines, lineIndex) {
	const line = lines[lineIndex++];
	const [lineKind, l] = splitSemicolons(line);
	console.log({lineIndex, lineKind, l})
	switch (lineKind) {
		case 13: return parse13(l, lines, lineIndex);
		case 15: return parse15(l, lines, lineIndex);
		case 17: return parse17(l, lines, lineIndex);
		// NOTE: There are also 23 and 25 lines, but I don't know what they're for.
		default: return [lineIndex, undefined];
	}
}

/**
 * Parse a carrier object; carrier lines begin with "13"
 *
 * @param {array} l - array of string representing the elements of the current line
 * @param {array} lines - array of lines from the Carrier.cfg
 * @param {number} lineIndex - the current line to inspect
 * @return {array} a pair [lineIndex2, model], where lineIndex2 is the new lineIndex and model is a Carrier.
 */
function parse13(l, lines, lineIndex) {
	const sName = l[0];
	const l1 = l[1].split("/");
	const sId = l1[0];
	//val sBarcode = l1(1)
	const id = parseInt(sId);
	const nSites = parseInt(l[4]);
	const deviceNameList = parse998(lines[lineIndex + nSites + 1]);
	const deviceName = (deviceNameList.length != 1) ? undefined : deviceNameList[0];
	const partNoList = parse998(lines[lineIndex + nSites + 3]);
	const partNo = (partNoList.length != 1) ? undefined : partNoList[0];
	return [lineIndex + nSites + 6, new Carrier(sName, id, nSites, deviceName, partNo)];
}

/**
 * Parse a labware object; labware lines begin with "15"
 *
 * @param {array} l - array of string representing the elements of the current line
 * @param {array} lines - array of lines from the Carrier.cfg
 * @param {number} lineIndex - the current line to inspect
 * @return {array} a pair [lineIndex2, model], where lineIndex2 is the new lineIndex and model is a EvowareLabwareModel.
 */
function parse15(l, lines, lineIndex) {
	const sName = l[0];
	const ls2 = l[2].split("/");
	const nCols = parseInt(ls2[0]);
	const nRows = parseInt(ls2[1]);
	//const nCompartments = ls2(2).toInt
	const ls4 = l[4].split("/")
	const zBottom = parseInt(ls4[0]);
	const zDispense = parseInt(ls4[2]);
	const nArea = Number(l[5]); // mm^2
	const nDepthOfBottom = Number(l[15]); // mm
	//const nTipsPerWell = l(6).toDouble
	//const nDepth = l(15).toDouble // mm
	const nCarriers = parseInt(l[20]);
	// shape: flat, round, v-shaped (if nDepth == 0, then flat, if > 0 then v-shaped, if < 0 then round
	// labware can have lid

	// negative values for rounded bottom, positive for cone, 0 for flat
	const [nDepthOfCone, nDepthOfRound] = (nDepthOfBottom > 0)
	 	? [nDepthOfBottom, 0.0]
		: [0.0, -nDepthOfBottom];
	const r = Math.sqrt(nArea / Math.PI);
	// Calculate the volume in microliters
	const ul = ((zBottom - zDispense) / 10.0 - nDepthOfCone - nDepthOfRound) * nArea +
		// Volume of a cone: (1/3)*area*height
		(nDepthOfCone * nArea / 3) +
		// Volume of a half-sphere:
		((4.0 / 6.0) * Math.PI * r * r * r);

	const lsCarrier = lines.slice(lineIndex, lineIndex + nCarriers);
	const sites = _.flatten(lsCarrier.map(s => {
		const ls = parse998(s); // split line, but drop the "998" prefix
		const idCarrier = parseInt(ls[0]);
		const sitemask = ls[1];
		const [, , site_li] = Utils.parseEncodedIndexes(sitemask)
		return site_li.map(site_i => new CarrierSiteIndex(idCarrier, site_i));
	}));

	[lineIndex + 10 + nCarriers, new EvowareLabwareModel(sName, nRows, nCols, ul, sites)];
}

/**
 * Parse a vector object; vector lines begin with "17"
 *
 * @param {array} l - array of string representing the elements of the current line
 * @param {array} lines - array of lines from the Carrier.cfg
 * @param {number} lineIndex - the current line to inspect
 * @return {array} a pair [lineIndex2, model], where lineIndex2 is the new lineIndex and model is a EvowareLabwareModel.
 */
function parse17(l, lines, lineIndex) {
	//println("parse17: "+l.toList)
	const l0 = l[0].split("_");
	if (l0.length < 3)
		return [lineIndex, undefined];

	const sClass = l0[1];
	const iRoma = parseInt(l0[2]) - 1;
	const nSteps = parseInt(l[3]);
	const idCarrier = parseInt(l[4]);
	const model = (nSteps > 2) ? new Vector(idCarrier, sClass, iRoma) : undefined;
	return [lineIndex + nSteps, model];
}

/**
 * Parse a line with the expected lineType=998.  Discards the linetype and just returns a list of strings elements.
 * @param  {string} s - the line
 * @return {array} array of line elements
 */
function parse998(s) {
	const [lineType, l] = splitSemicolons(s);
	assert(lineType === 998);
	return _.initial(l);
}
