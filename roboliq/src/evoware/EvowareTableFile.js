import _ from 'lodash';
import assert from 'assert';
//import {sprintf} from 'sprintf-js';
import EvowareUtils from './EvowareUtils.js';
import * as EvowareCarrierFile from './EvowareCarrierFile.js';

/**
 * Represents the table setup for an Evoware script file.
 * @param {EvowareCarrierData} carrierData
 * @param {array} carrierIdsInternal - array of carrier IDs, whereby the index in the array represents the gridIndex, and a value of -1 indicates no carrier at that grid point
 * @param {array} hotelObjects - array of HotelObjects
 * @param {array} externalObjects - array of ExternalObjects
 * @param {object} layout - map from carrierName to gridIndex to siteIndex to {label, labwareModelName}
 */
export class EvowareTableData {
	constructor(carrierIdsInternal, hotelObjects, externalObjects, layout) {
		this.carrierIdsInternal = carrierIdsInternal;
		this.hotelObjects = hotelObjects;
		this.externalObjects = externalObjects;
		this.layout = layout;
	}
}

/**
 * @param {integer} parentCarrierId - carrier ID for the carrier holding this hotel
 * @param {integer} gridIndex - grid index of the hotel
 */
export class HotelObject {
	constructor(parentCarrierId, gridIndex) {
		this.parentCarrierId = parentCarrierId;
		this.gridIndex = gridIndex;
	}
}

/**
 * @param {integer} n1 - Value of unknown significance
 * @param {integer} n2 - Value of unknown significance
 * @param {string} carrierName - carrier name for the carrier holding this hotel/object
 */
export class ExternalObject {
	constructor(n1, n2, carrierName) {
		this.n1 = n1;
		this.n2 = n2;
		this.carrierName = carrierName;
	}
}

/**
 * Parses an Evoware `.esc` script file, extracting the table setup.
 * @param {EvowareCarrierData} carrierData
 * @param {string} filename
 */
export function load(carrierData, filename) {
	const lines = new EvowareUtils.EvowareSemicolonFile(filename, 7);
	lines.next() // TODO: should we check whether this is equal to "--{ RPG }--"?
	//println(lsLine.takeWhile(_ != "--{ RPG }--").length)
	const [, l] = lines.nextSplit();
	lines.skip(0);
	const tableFile = parse14(carrierData, l, lines);
	//println("parseFile: "+rest.takeWhile(_ != "--{ RPG }--"))
	return tableFile;
}

/**
 * Parse a table.
 *
 * @param {EvowareCarrierData} carrierData
 * @param {array} l - array of string representing the elements of the current line
 * @param {EvowareSemicolonFile} lines - array of lines from the Carrier.cfg
 * @return {EvowareTableData}
 */
function parse14(carrierData, l, lines) {
	//import configFile._
	const carrierIdsInternal = parse14_getCarrierIds(_.initial(l));
	console.log("carrierIdsInternal: "+JSON.stringify(carrierIdsInternal));
	const internalObjects = parse14_getLabwareObjects(carrierData, carrierIdsInternal, lines);
	console.log("internalObjects: "+JSON.stringify(internalObjects));
	const hotelObjects = parse14_getHotelObjects(lines);
	const externalObjects = parse14_getExternalObjects(lines);
	const externalSiteIdToLabwareModelName = parse14_getExternalLabwares(lines);
	const externalCarrierNameToGridIndexList = parse14_getExternalCarrierGrids(externalObjects, lines);

	// FIXME: for debug only
	const gridToCarrierIdInternal = _(carrierIdsInternal).map((id, index) => [index.toString(), id]).filter(([, id]) => id > -1).zipObject().value();
	console.log("gridToCarrierIdInternal: "+JSON.stringify(gridToCarrierIdInternal));
	// ENDFIX
	/*
	* @param {array} carrierIdsInternal - List with optional CarrierID for each grid on the table
    * @param {array} hotelObjects - array of HotelObjects
    * @param {array} externalObjects - array of ExternalObjects
    * @param {object} carrierIdToGrids - map from carrierId to grid indexes where that carrier is used
    * @param {object} siteIdToLabel - map from carrierIndex to gridIndex to siteIndex to name of the site
    * @param {object} siteIdToLabwareModel - map from carrierIndex to gridIndex to siteIndex to labware model at the site
    */

	const carrierIdToGrids = {};
	// Add internal carriers
	carrierIdsInternal.forEach((carrierId, gridIndex) => {
		if (carrierId > -1)
			carrierIdToGrids[carrierId] = [gridIndex];
	});
	// Add external carriers
	externalCarrierNameToGridIndexList.forEach(([carrierName, gridIndex]) => {
		const carrier = carrierData.nameToCarrier[carrierName];
		if (_.isUndefined(carrier)) {
			console.log(`unknown carrier ${carrierName} at grid ${gridIndex}`)
		}
		else {
			const l = _.get(carrierIdToGrids, carrier.id, []);
			l.push(gridIndex);
			carrierIdToGrids[carrier.id] = _.uniq(l);
		}
	});

	function set(carrierName, gridIndex, siteIndex, propertyName, propertyValue) {
		const c = _.get(layout, carrierName, {});
		if (_.isUndefined(gridIndex)) {
			_.set(c, propertyName, propertyValue);
		}
		else {
			const g = _.get(c, gridIndex, {});
			if (_.isUndefined(siteIndex)) {
				_.set(g, propertyName, propertyValue);
			}
			else {
				const s = _.get(g, siteIndex, {});
				_.set(s, propertyName, propertyValue);
				_.set(g, siteIndex, s);
			}
			_.set(c, gridIndex, g);
		}
		_.set(layout, carrierName, c);
	}

	// Get list of all carriers on the table
	const carrierAndGridList = [];
	// Internal carriers
	carrierIdsInternal.forEach((carrierId, gridIndex) => {
		if (carrierId > -1) {
			const carrier = carrierData.idToCarrier[carrierId];
			carrierAndGridList.push([carrier.name, gridIndex, "internal"]);
		}
	});
	// Hotel carriers
	hotelObjects.forEach(o => {
		const carrier = carrierData.idToCarrier[o.parentCarrierId];
		carrierAndGridList.push([carrier.name, o.gridIndex, "hotel"]);
	});
	// External objects
	externalCarrierNameToGridIndexList.forEach(([carrierName, gridIndex]) => {
		carrierAndGridList.push([carrierName, gridIndex, "external"]);
	});

	// Sort the list by gridIndex
	const carrierAndGridList1 = _.sortBy(carrierAndGridList, l => l[1]);
	//console.log(JSON.stringify(carrierAndGridList1, null, '\t'));

	// Populate the carrier/grid layout information in gridIndex-order
	const layout = {};
	carrierAndGridList1.forEach(([carrierName, gridIndex, propertyName]) => {
		set(carrierName, gridIndex, undefined, propertyName, true);
	});

	internalObjects.forEach(([carrierName, gridIndex, siteIndex, label, labwareModelName]) => {
		if (!_.isEmpty(label))
			set(carrierName, gridIndex, siteIndex, 'label', label);
		set(carrierName, gridIndex, siteIndex, 'labwareModelName', labwareModelName);
	});

	hotelObjects.forEach(o => {
		const carrier = carrierData.idToCarrier[o.parentCarrierId];
		set(carrier.name, o.gridIndex, undefined, 'hotel', true);
	});

	return new EvowareTableData(
		carrierIdsInternal,
		hotelObjects,
		externalObjects,
		layout
	);
/*
	const mapSiteToLabel = lTableInfo.map(o => o._1 -> o._2).toMap
	const siteIdExternalToLabwareModel_l: List[(CarrierGridSiteIndex, EvowareLabwareModel)] = (
		mapSiteToExternalLabwareModel.toList.flatMap { case (CarrierSite(carrier, siteIndex), model) =>
			mapCarrierToGrid2.get(carrier).map(gridIndex => CarrierGridSiteIndex(carrier.id, gridIndex, siteIndex) -> model)
		}
	)
	const siteIdToLabwareModel_m: Map[CarrierGridSiteIndex, EvowareLabwareModel] = (
		lTableInfo.map(o => o._1 -> o._3) ++
		siteIdExternalToLabwareModel_l
	).toMap
	const mapCarrierToGrid1 = lCarrier_?.zipWithIndex.collect({ case (Some(o), iGrid) => o -> iGrid }).toMap
	const mapCarrierToGrid = mapCarrierToGrid1 ++ mapCarrierToGrid2

	const carrierIdToGrids_m: Map[Int, List[Int]] = {
		const l =
			gridToInternalCarrierId_m.toList.map(_.swap) ++
			mapCarrierToGrid2.toList.map({ case (carrier, gridIndex) => (carrier.id, gridIndex)})
		l.groupBy(_._1).mapValues(_.map(_._2))
	}

	const tableFile = new EvowareTableData(
		configFile,
		carrierId_l.toVector,
		lHotelObject,
		lExternalObject,
		carrierIdToGrids_m,
		//mapCarrierToGrid,
		mapSiteToLabel,
		siteIdToLabwareModel_m
	)

	console.log("tablefile:")
	console.log(tableFile.toDebugString())
	return tableFile;*/
}

/**
 * Extract array where the array index is the grid index and the value is the carrier ID.
 * This information is on the first line of the table definition.
 * A -1 value for the carrier ID means that there is no carrier at that grid.
 * @param  {array} l - elements of line
 * @return {array} array of carrier IDs on this table
 */
function parse14_getCarrierIds(l) {
	return l.map(s => parseInt(s));
}

/**
 * Get array of labwares on the table.
 * @param  {EvowareCarrierData} carrierData
 * @param  {EvowareSemicolonFile} lines - lines of table file
 * @return {array} an array of tuples (carrier name, gridIndex, siteIndex, site label, labware model name)
 */
function parse14_getLabwareObjects(carrierData, carrierIdsInternal, lines) {
	const result = [];
	carrierIdsInternal.forEach((carrierId, gridIndex) => {
		if (carrierId > -1) {
			const carrier = carrierData.idToCarrier[carrierId];
			const [n0, l0] = lines.nextSplit();
			const [n1, l1] = lines.nextSplit();
			//console.log({n0, l0, n1, l1, carrier})
			assert(n0 === 998 && n1 === 998 && parseInt(l0[0]) === carrier.siteCount);
			//println(iGrid+": "+carrier)
			_.times(carrier.siteCount, siteIndex => {
				const labwareModelName = l0[siteIndex+1];
				if (!_.isEmpty(labwareModelName)) {
					const item = [carrier.name, gridIndex, siteIndex, l1[siteIndex], labwareModelName];
					result.push(item);
				}
			});
		}
		else {
			lines.skip(1);
		}
	});
	return result;
}

/**
 * Parse the hotel objects
 * @param  {[type]} lsLine:         List[String]  [description]
 * @return {array} an array of HotelObjects
 */
function parse14_getHotelObjects(lines) {
	const [n0, l0] = lines.nextSplit();
	assert(n0 === 998);
	const count = parseInt(l0[0]);
	return _.times(count, () => {
		const [n, l] = lines.nextSplit();
		assert(n == 998);
		const id = parseInt(l[0]);
		const iGrid = parseInt(l[1]);
		return new HotelObject(id, iGrid);
	});
}

/**
 * Parse the external objects.
 * @param  {[type]} lines       [description]
 * @return {array} an array of external objects
 */
function parse14_getExternalObjects(lines) {
	const [n0, l0] = lines.nextSplit();
	assert(n0 === 998);
	const count = parseInt(l0[0]);
	return _.times(count, () => {
		const [n, l] = lines.nextSplit();
		assert(n == 998);
		const n1 = parseInt(l[0]);
		const n2 = parseInt(l[1]);
		const carrierName = l[2];
		return new ExternalObject(n1, n2, carrierName);
	});
}

/**
 * Parse labware on external sites
 * @param  {[type]} lines       [description]
 * @return {object} map from carrier ID to site ID (which is 0 in this case) to labware model name
 */
function parse14_getExternalLabwares(lines) {
	const [n0, l0] = lines.nextSplit();
	assert(n0 === 998);
	const count = parseInt(l0[0]);
	const result = {};
	for (let i = 0; i < count; i++) {
		const [n, l] = lines.nextSplit();
		assert(n == 998);
		const carrierId = parseInt(l[0]);
		const labwareModelName = l[1];
		_.set(result, [carrierId.toString(), "0"], labwareModelName);
	}
	return result;
}

function parse14_getExternalCarrierGrids(externalObjects, lines) {
	return externalObjects.map(external => {
		const [n, l] = lines.nextSplit();
		assert(n === 998);
		//console.log("carrierName: "+external.carrierName);
		// we need to force the system liquid to be on grid -1
		const gridIndex = (external.carrierName === "System") ? -1 : parseInt(l[0]);
		return [external.carrierName, gridIndex];
	});
}
