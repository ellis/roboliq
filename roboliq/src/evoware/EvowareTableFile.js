import _ from 'lodash';
import assert from 'assert';
//import {sprintf} from 'sprintf-js';
import EvowareUtils from './EvowareUtils.js';
import * as EvowareCarrierFile from './EvowareCarrierFile.js';
import M from '../Medley.js';

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
 * @param {integer} n1 - Value of unknown significance (4 for System, 0 for others?)
 * @param {integer} n2 - I think this is the on-screen display index
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
 * @return {object} a table layout, keys are carrier names, sub-keys are gridIndexes or properties, sub-sub-keys are siteIndexes or property, and sub-sub-sub-keys {label, labwareModelName}
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
 * @return {object} a table layout, keys are carrier names, sub-keys are gridIndexes or properties, sub-sub-keys are siteIndexes or property, and sub-sub-sub-keys {label, labwareModelName}
 */
function parse14(carrierData, l, lines) {
	//import configFile._
	const carrierIdsInternal = parse14_getCarrierIds(_.initial(l));
	//console.log("carrierIdsInternal: "+JSON.stringify(carrierIdsInternal));
	const internalObjects = parse14_getLabwareObjects(carrierData, carrierIdsInternal, lines);
	//console.log("internalObjects: "+JSON.stringify(internalObjects));
	const hotelObjects = parse14_getHotelObjects(lines);
	const externalObjects = parse14_getExternalObjects(lines);
	const externalSiteIdToLabwareModelName = parse14_getExternalLabwares(lines);
	const externalCarrierNameToGridIndexList = parse14_getExternalCarrierGrids(externalObjects, lines);

	// FIXME: for debug only
	//const gridToCarrierIdInternal = _(carrierIdsInternal).map((id, index) => [index.toString(), id]).filter(([, id]) => id > -1).fromPairs().value();
	//console.log("gridToCarrierIdInternal: "+JSON.stringify(gridToCarrierIdInternal));
	// ENDFIX

	function set(carrierName, gridIndex, siteIndex, propertyName, propertyValue) {
		const c = _.get(layout, carrierName, {});
		if (_.isUndefined(gridIndex)) {
			M.setMut(c, propertyName, propertyValue);
		}
		else {
			const g = _.get(c, gridIndex, {});
			if (_.isUndefined(siteIndex)) {
				M.setMut(g, propertyName, propertyValue);
			}
			else {
				const s = _.get(g, siteIndex, {});
				M.setMut(s, propertyName, propertyValue);
				M.setMut(g, siteIndex, s);
			}
			M.setMut(c, gridIndex, g);
		}
		M.setMut(layout, carrierName, c);
	}

	// Get list of all carriers on the table
	const carrierAndGridList = [];
	// Internal carriers
	carrierIdsInternal.forEach((carrierId, gridIndex) => {
		if (carrierId > -1) {
			const carrier = carrierData.getCarrierById(carrierId);
			carrierAndGridList.push([carrier.name, gridIndex, "internal", true]);
		}
	});
	// Hotel carriers
	hotelObjects.forEach(o => {
		const carrier = carrierData.getCarrierById(o.parentCarrierId);
		carrierAndGridList.push([carrier.name, o.gridIndex, "hotel", true]);
	});
	// External objects
	externalObjects.forEach((external, i) => {
		const [carrierName, gridIndex] = externalCarrierNameToGridIndexList[i];
		carrierAndGridList.push([carrierName, gridIndex, "external", _.pick(external, 'n1', 'n2')]);
	});

	// Sort the list by gridIndex
	const carrierAndGridList1 = _.sortBy(carrierAndGridList, l => l[1]);
	//console.log(JSON.stringify(carrierAndGridList1, null, '\t'));

	// Populate the carrier/grid layout information in gridIndex-order
	const layout = {};
	carrierAndGridList1.forEach(([carrierName, gridIndex, propertyName, propertyValue]) => {
		set(carrierName, gridIndex, undefined, propertyName, propertyValue);
	});

	// Add to layout the internal site labels and labware
	internalObjects.forEach(([carrierName, gridIndex, siteIndex, label, labwareModelName]) => {
		if (!_.isEmpty(label))
			set(carrierName, gridIndex, siteIndex, 'label', label);
		set(carrierName, gridIndex, siteIndex, 'labwareModelName', labwareModelName);
	});
	// Add to layout the external site labware
	externalSiteIdToLabwareModelName.forEach(([carrierId, labwareModelName]) => {
		const carrier = carrierData.getCarrierById(carrierId);
		const result = _.find(externalCarrierNameToGridIndexList, ([carrierName,]) => carrierName === carrier.name);
		assert(!_.isUndefined(result));
		const [, gridIndex] = result;
		set(carrier.name, gridIndex, 1, 'labwareModelName', labwareModelName);
	});

	return layout;
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
			const carrier = carrierData.getCarrierById(carrierId);
			const [n0, l0] = lines.nextSplit();
			const [n1, l1] = lines.nextSplit();
			//console.log({n0, l0, n1, l1, carrierId, carrierName: carrierData.carrierIdToName[carrierId], carrier})
			assert(n0 === 998 && n1 === 998 && parseInt(l0[0]) === carrier.siteCount);
			//println(iGrid+": "+carrier)
			_.times(carrier.siteCount, siteIndex => {
				const labwareModelName = l0[siteIndex+1];
				if (!_.isEmpty(labwareModelName)) {
					const item = [carrier.name, gridIndex, siteIndex+1, l1[siteIndex], labwareModelName];
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
 * @param  {EvowareSemicolonFile} lines - lines of table file
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
 * @param  {EvowareSemicolonFile} lines - lines of table file
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
 * @param  {EvowareSemicolonFile} lines - lines of table file
 * @return {object} list of tuples (carrier ID, labware model name)
 */
function parse14_getExternalLabwares(lines) {
	const [n0, l0] = lines.nextSplit();
	assert(n0 === 998);
	const count = parseInt(l0[0]);
	return _.times(count, i => {
		const [n, l] = lines.nextSplit();
		assert(n == 998);
		const carrierId = parseInt(l[0]);
		const labwareModelName = l[1];
		return [carrierId, labwareModelName];
	});
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

/**
 * Create a string representation of an Evoware table layout
 * @param  {EvowareCarrierData} carrierData - data loaded from an evoware carrier file
 * @param  {object} table - a table layout, keys are carrier names, sub-keys are gridIndexes or properties, sub-sub-keys are siteIndexes or property, and sub-sub-sub-keys {label, labwareModelName}
 * @return {string} string representation of table layout
 */
export function toStrings(carrierData, table) {
	const l1 = [
		"00000000",
		"20000101_000000 No log in       ",
		"                                                                                                                                ",
		"No user logged in                                                                                                               ",
		"--{ RES }--",
		"V;200",
		"--{ CFG }--",
		"999;219;32;"
	];
	const s2 = toString_internalCarriers(carrierData, table);
	const l3 = toStrings_internalLabware(carrierData, table);
	const l4 = toStrings_hotels(carrierData, table);
	const l5 = toStrings_externals(carrierData, table);
	const l6 = [
		"996;0;0;",
		"--{ RPG }--"
	];
	const l = _.flatten([l1, s2, l3, l4, l5, l6]);
	//console.log(l.join("\n"));
	return l;
}

/**
 * Create a string representation of the internal carriers
 * @param  {EvowareCarrierData} carrierData - data loaded from an evoware carrier file
 * @param  {object} table - a table layout, keys are carrier names, sub-keys are gridIndexes or properties, sub-sub-keys are siteIndexes or property, and sub-sub-sub-keys {label, labwareModelName}
 * @return {string} string representation of internal carriers
 */
export function toString_internalCarriers(carrierData, table) {
	// Get list [[gridIndex, carrier.id]] for internal sites
	// [[a, b]]
	const gridToCarrierName_l = _(table).map((c, carrierName) => {
		//console.log({c, carrierName})
		return _.map(c, (x, gridIndexText) => {
			if (x.internal) {
				try {
					const gridIndex = parseInt(gridIndexText);
					return [gridIndex, carrierName];
				} catch(e) {
					// Do nothing
				}
			}
			return undefined;
		});
	}).flatten().compact().value();
	const gridToCarrierName_m = _.fromPairs(gridToCarrierName_l);
	//console.log({gridToCarrierId_l, gridToCarrierId_m})

	const l = _.times(99, gridIndex => {
		const carrierName = gridToCarrierName_m[gridIndex];
		return (_.isEmpty(carrierName)) ? -1 : carrierData.getCarrierByName(carrierName).id;
	});

	return `14;${l.join(";")};`;
}

/**
 * Create a string representation of the internal labware
 * @param  {EvowareCarrierData} carrierData - data loaded from an evoware carrier file
 * @param  {object} table - a table layout, keys are carrier names, sub-keys are gridIndexes or properties, sub-sub-keys are siteIndexes or property, and sub-sub-sub-keys {label, labwareModelName}
 * @return {string} string representation of internal labware
 */
export function toStrings_internalLabware(carrierData, table) {
	const items0 = [];
	_.forEach(table, (c, carrierName) => {
		_.forEach(c, (g, gridIndexText) => {
			if (g.internal === true) {
				const carrierId = _.get(carrierData.getCarrierByName(carrierName), 'id', -1);
				items0.push({carrierName, carrierId, gridIndex: parseInt(gridIndexText), g});
			}
		});
	});
	// Sort by gridIndex
	const items = _.sortBy(items0, 'gridIndex');
	//console.log("items:")
	//console.log(items)
	const gridIndexToItem = _(items).map(x => [x.gridIndex, x]).fromPairs().value();
	//console.log({gridIndexToItem})

	return _.flatten(_.times(99, gridIndex => {
		const item = gridIndexToItem[gridIndex];
		if (_.isUndefined(item)) {
			return "998;0;";
		}
		else {
			const carrier = carrierData.getCarrierByName(item.carrierName);
			//console.log({item, carrier})
			//val sSiteCount = if (carrier.nSites > 0) carrier.nSites.toString else ""
			const namesAndLabels = _.times(carrier.siteCount, siteIndex => {
				//console.log({g: item.g})
				const labwareModelName = _.get(item.g, [siteIndex + 1, 'labwareModelName'], "");
				const label = _.get(item.g, [siteIndex + 1, 'label'], "");
				return {labwareModelName, label};
			})
			return [
				`998;${carrier.siteCount};${_.map(namesAndLabels, x => x.labwareModelName).join(';')};`,
				`998;${_.map(namesAndLabels, x => x.label).join(';')};`,
			]
		}
	}));
}

/**
 * Create a string representation of the hotels
 * @param  {EvowareCarrierData} carrierData - data loaded from an evoware carrier file
 * @param  {object} table - a table layout, keys are carrier names, sub-keys are gridIndexes or properties, sub-sub-keys are siteIndexes or property, and sub-sub-sub-keys {label, labwareModelName}
 * @return {string} string representation of hotels
 */
export function toStrings_hotels(carrierData, table) {
	const hotelItems0 = [];
	_.forEach(table, (c, carrierName) => {
		_.forEach(c, (g, gridIndexText) => {
			if (g.hotel === true) {
				const carrierId = carrierData.getCarrierByName(carrierName).id;
				hotelItems0.push([carrierId, parseInt(gridIndexText)]);
			}
		});
	});
	//console.log({hotelItems0});
	const hotelItems = _.sortBy(hotelItems0, x => x[1]);
	return _.flatten([
		`998;${hotelItems.length};`,
		hotelItems.map(([carrierId, gridIndex]) => `998;${carrierId};${gridIndex};`)
	]);
}

/**
 * Create a string representation of external carriers
 * @param  {EvowareCarrierData} carrierData - data loaded from an evoware carrier file
 * @param  {object} table - a table layout, keys are carrier names, sub-keys are gridIndexes or properties, sub-sub-keys are siteIndexes or property, and sub-sub-sub-keys {label, labwareModelName}
 * @return {string} string representation of external carriers
 */
export function toStrings_externals(carrierData, table) {
	const items0 = [];
	_.forEach(table, (c, carrierName) => {
		_.forEach(c, (g, gridIndexText) => {
			if (g.external) {
				const carrierId = _.get(carrierData.getCarrierByName(carrierName), 'id', -1);
				items0.push({carrierName, carrierId, gridIndex: parseInt(gridIndexText), g});
			}
		});
	});
	// Sort by carrierId
	const items = _.sortBy(items0, 'carrierId');
	// Generate list of external objects and their carriers
	const l1 = _.flatten([
		`998;${items.length};`,
		items.map(({carrierName, g}) => `998;${g.external.n1};${g.external.n2};${carrierName};`)
	]);
	// Generate list of labware models
	const itemsWithLabware = items.filter(item => _.has(item, "g.1.labwareModelName"));
	const l2 = _.flatten([
		`998;${itemsWithLabware.length};`,
		itemsWithLabware.map(({carrierId, g}) => `998;${carrierId};${_.get(g, "1.labwareModelName")};`)
	]);
	// Generate grid list
	const l3 = items.map(({gridIndex}) => `998;${(gridIndex === -1) ? 1 : gridIndex};`);

	return _.concat(l1, l2, l3);
}
