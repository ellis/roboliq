/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Loads data from an Evoware carrier file.
 * @module
 */

import _ from 'lodash';
import assert from 'assert';
//import fs from 'fs';
//import iconv from 'iconv-lite';
import {sprintf} from 'sprintf-js';
import * as EvowareUtils from './EvowareUtils.js';


/**
 * Tuple for location refered to by carrier+grid+site indexes
 * @class module:evoware/EvowareCarrierFile.CarrierGridSiteIndex
 * @property {integer} carrierId - ID for the carrier
 * @property {integer} gridIndex - 1-based index of grid
 * @property {integer} siteIndex -  0-based index of site
 */

export class CarrierGridSiteIndex {
	constructor(carrierId, gridIndex, siteIndex) {
		this.carrierId = carrierId;
		this.gridIndex = gridIndex;
		this.siteIndex = siteIndex;
	}
}

/**
 * Tuple for location refered to by carrier+site index
 * @class module:evoware/EvowareCarrierFile.CarrierSiteIndex
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
 * A base type for evoware models, one of Carrier, LabwareModel, or Vector.
 * @typedef {object} EvowareModel
 * @property {string} type - the type of model
 */

/**
 * A Carrier object
 * @class module:evoware/EvowareCarrierFile.Carrier
 * @property {string} type - should be "Carrier"
 * @property {string} name
 * @property {integer} id
 * @property {integer} siteCount
 * @property {string} [deviceName]
 * @property {string} [partNo]
 * @property {array} [vectors] - array of vector names for this carrier
 */
export class Carrier {
	constructor(name, id, siteCount, deviceName, partNo) {
		this.type = "Carrier";
		this.name = name;
		this.id = id;
		this.siteCount = siteCount;
		this.deviceName = deviceName;
		this.partNo = partNo;
		this.vectors = [];
	}
}

/**
 * An evoware labware model
 * @class module:evoware/EvowareCarrierFile.LabwareModel
 * @property {string} type - should be "LabwareModel"
 * @property {string} name
 * @property {integer} rows
 * @property {integer} cols
 * @property {number} ul - maximum volume of wells
 * @property {array} sites - list of CarrierSiteIndexes where this labware can be placed.
 */

export class LabwareModel {
	constructor(name, rows, cols, ul, sites) {
		this.type = "LabwareModel";
		this.name = name;
		this.rows = rows;
		this.cols = cols;
		this.ul = ul;
		this.sites = sites;
	}
}

/**
 * A tranporter "vector", related to movements that the RoMas can make
 * @class module:evoware/EvowareCarrierFile.Vector
 * @property {string} type - should be "Vector"
 * @property {integer} carrierId - which carrier this vector is for
 * @property {string} clazz - Wide, Narrow, or user-defined
 * @property {integer} romaId - which RoMa this vector is for
 */

export class Vector {
	constructor(carrierId, clazz, romaId) {
		this.type = "Vector";
		this.carrierId = carrierId;
		this.clazz = clazz;
		this.romaId = romaId;
	}
}

/**
 * An object representing an evoware carrier file
 *
 * @class module:evoware/EvowareCarrierFile.EvowareCarrierData
 * @property {object} models - map from model name to model data
 * @property {object} idToName - map of model ID to model name
 * @property {object} carrierIdToVectors - map of carrier ID to list of Vectors
 */

export class EvowareCarrierData {
	constructor(carrierModels, labwareModels) {
		this.carrierModels = carrierModels;
		this.labwareModels = labwareModels;
		this.carrierIdToName = _(carrierModels).map(x => [x.id, x.name]).fromPairs().value();
		this.labwareIdToName = _(labwareModels).map(x => [x.id, x.name]).fromPairs().value();
	}

	getCarrierByName(carrierName) {
		return this.carrierModels[carrierName];
	}

	getCarrierById(id) {
		const name = this.carrierIdToName[id];
		return this.carrierModels[name];
	}

	/**
	 * Print debug output: carrier id, carrier name.
	 */
	printCarriersById() {
		const l0 = _(this.carrierModels).map(model => [model.id, model.name]).value();
		const l = _.sortBy(l0, x => x[0]);
		//console.log({l})
		l.forEach(([id, name]) => {
			console.log(sprintf("%03d\t%s", id, name));
		});
	}
}

/*
case class CarrierSite(
	carrier: Carrier,
	iSite: Int
)
*/

/**
 * Load an evoware carrier file and return its model data.
 * @param  {string} filename - path to the carrier file
 * @return {EvowareCarrierData}
 */
export function load(filename) {
	const modelList = loadEvowareModels(filename);
	const data = makeEvowareCarrierData(modelList);
	//console.log({data});
	return data;
}

/**
 * Create an EvowareCarrierData object from an array of evoware models.
 * @param  {array} modelList - array of evoware models
 * @return {EvowareCarrierData}
 */
function makeEvowareCarrierData(modelList) {
	// Create maps/lists for the various model types
	const carrierModels = {};
	const labwareModels = {};
	const vectors = [];
	_.forEach(modelList, model => {
		if (model.type === "Carrier") carrierModels[model.name] = model;
		else if (model.type === "LabwareModel") labwareModels[model.name] = model;
		else vectors.push(model);
	});
	// Create map from ID to name
	const idToName = _(carrierModels).filter(x => x.type === "Carrier").map(model => [model.id, model.name]).fromPairs().value();
	// Add vectors to carriers
	const carrierIdToVectors = _.groupBy(vectors, 'carrierId');
	_.forEach(carrierIdToVectors, (vectors, carrierId) => {
		const carrierName = idToName[carrierId];
		const carrier = carrierModels[carrierName];
		carrier.vectors = vectors.map(x => x.name);
	});
	//console.log({modelList, models, idToName, carrierIdToVectors})

	return new EvowareCarrierData(
		carrierModels,
		labwareModels
	);
}

/**
 * Parses the file `Carrier.cfg` into a list of `EvowareModels`.
 * @param {string} filename - path to the carrier file
 * @return {array} an array of EvowareModels (e.g. Carriers, Vectors, EvowareLabwareModels)
 */
function loadEvowareModels(filename) {
	const models = [];

	const lines = new EvowareUtils.EvowareSemicolonFile(filename, 4);

	// Find models in the carrier file
	while (lines.hasNext()) {
		const model = parseModel(lines);
		//console.log({model})
		if (!_.isUndefined(model))
			models.push(model)
		//assert(lineIndex2 > lineIndex);
		//console.log({lineIndex2})
	}

	return models;
}

/**
 * Parse the line and return a model, if relevant.
 * @param {EvowareSemicolonFile} lines - array of lines from the Carrier.cfg
 * @return {array} an optional model.
 */
function parseModel(lines) {
	const [lineKind, l] = lines.nextSplit();
	//console.log({lineKind, l})
	switch (lineKind) {
		case 13: return parse13(l, lines);
		case 15: return parse15(l, lines);
		case 17: return parse17(l, lines);
		// NOTE: There are also 23 and 25 lines, but I don't know what they're for.
		default: return undefined;
	}
}

/**
 * Parse a carrier object; carrier lines begin with "13"
 *
 * @param {array} l - array of string representing the elements of the current line
 * @param {EvowareSemicolonFile} lines - array of lines from the Carrier.cfg
 * @return {Carrier} a Carrier.
 */
function parse13(l, lines) {
	const sName = l[0];
	const l1 = l[1].split("/");
	const sId = l1[0];
	//val sBarcode = l1(1)
	const id = parseInt(sId);
	const nSites = parseInt(l[4]);
	const deviceNameList = parse998(lines.peekAhead(nSites + 1));
	const deviceName = (deviceNameList.length != 1) ? undefined : deviceNameList[0];
	const partNoList = parse998(lines.peekAhead(nSites + 3));
	const partNo = (partNoList.length != 1) ? undefined : partNoList[0];
	lines.skip(nSites + 6);
	return new Carrier(sName, id, nSites, deviceName, partNo);
}

/**
 * Parse a labware object; labware lines begin with "15"
 *
 * @param {array} l - array of string representing the elements of the current line
 * @param {EvowareSemicolonFile} lines - array of lines from the Carrier.cfg
 * @return {LabwareModel} a new LabwareModel.
 */
function parse15(l, lines) {
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

	const lsCarrier = lines.take(nCarriers);
	const sites = _.flatten(lsCarrier.map(s => {
		const ls = parse998(s); // split line, but drop the "998" prefix
		const idCarrier = parseInt(ls[0]);
		const sitemask = ls[1];
		const [, , site_li] = EvowareUtils.parseEncodedIndexes(sitemask);
		//console.log({sitemask, site_li})
		return site_li.map(site_i => new CarrierSiteIndex(idCarrier, site_i));
	}));

	lines.skip(10);
	return new LabwareModel(sName, nRows, nCols, ul, sites);
}

/**
 * Parse a vector object; vector lines begin with "17"
 *
 * @param {array} l - array of string representing the elements of the current line
 * @param {EvowareSemicolonFile} lines - array of lines from the Carrier.cfg
 * @return {Vector} a new Vector, if any
 */
function parse17(l, lines) {
	//println("parse17: "+l.toList)
	const l0 = l[0].split("_");
	if (l0.length < 3)
		return undefined;

	const sClass = l0[1];
	const iRoma = parseInt(l0[2]) - 1;
	const nSteps = parseInt(l[3]);
	const idCarrier = parseInt(l[4]);
	const model = (nSteps > 2) ? new Vector(idCarrier, sClass, iRoma) : undefined;
	lines.skip(nSteps);
	return model;
}

/**
 * Parse a line with the expected lineType=998.  Discards the linetype and just returns a list of strings elements.
 * @param  {string} s - the line
 * @return {array} array of line elements
 */
function parse998(s) {
	const [lineType, l] = EvowareUtils.splitSemicolons(s);
	assert(lineType === 998);
	return _.initial(l);
}
