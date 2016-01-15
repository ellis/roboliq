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
			carrierAndGridList.push([carrier.name, gridIndex, "internal", true]);
		}
	});
	// Hotel carriers
	hotelObjects.forEach(o => {
		const carrier = carrierData.idToCarrier[o.parentCarrierId];
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
		const carrier = carrierData.idToCarrier[carrierId];
		const result = _.find(externalCarrierNameToGridIndexList, ([carrierName,]) => carrierName === carrier.name);
		assert(!_.isUndefined(result));
		const [, gridIndex] = result;
		set(carrier.name, gridIndex, 0, 'labwareModelName', labwareModelName);
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

/*
export function toStringWithLabware(carrierData, table) {
		siteIdToLabel_m2 <- ResultC.map(siteToNameAndLabel_m) { case (cngsi, (label, _)) =>
			for {
				carrier <- ResultC.from(configFile.mapNameToCarrier.get(cngsi.carrierName), s"carrier `${cngsi.carrierName}` not present in the evoware config file")
			} yield {
				val siteId = CarrierGridSiteIndex(carrier.id, cngsi.gridIndex, cngsi.siteIndex - 1)
				val label2 = label.split('.').last
				siteId -> label2
			}
		}
		siteIdToLabwareModel_m2 <- ResultC.map(siteToNameAndLabel_m) { case (cngsi, (_, labwareModelName)) =>
			for {
				carrier <- ResultC.from(configFile.mapNameToCarrier.get(cngsi.carrierName), s"carrier `${cngsi.carrierName}` not present in the evoware config file")
				siteId = CarrierGridSiteIndex(carrier.id, cngsi.gridIndex, cngsi.siteIndex - 1)
				labwareModel <- ResultC.from(configFile.mapNameToLabwareModel.get(labwareModelName), s"labware model `${labwareModelName}` not present in the evoware config file")
			} yield siteId -> labwareModel
		}
	} yield {
		val siteIdToLabel_m3 = siteIdToLabel_m ++ siteIdToLabel_m2
		val siteIdToLabwareModel_m3 = siteIdToLabwareModel_m ++ siteIdToLabwareModel_m2
		//println("siteIdToLabwareModel_m:")
		//siteIdToLabwareModel_m3.foreach(println)
		// TODO: output current date and time
		// TODO: See whether we need to save the RES section when loading in the table
		// TODO: do we need to save values for the 999 line when loading the table?
		val l = List(
				"00000000",
				"20000101_000000 No log in       ",
				"                                                                                                                                ",
				"No user logged in                                                                                                               ",
				"--{ RES }--",
				"V;200",
				"--{ CFG }--",
				"999;219;32;"
			) ++
			toString_carriers(carrierData, table) ++
			toString_tableLabware(siteIdToLabel_m3, siteIdToLabwareModel_m3) ++
			toString_hotels() ++
			toString_externals() ++
			toString_externalLabware(siteIdToLabwareModel_m3) ++
			toString_externalGrids() ++
			List("996;0;0;", "--{ RPG }--")
		l.mkString("\n")
	}
}
*/

export function toString_carriers(carrierData, table) {
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

/*
private def toString_tableLabware(
	siteIdToLabel_m2: Map[CarrierGridSiteIndex, String],
	siteIdToLabwareModel_m2: Map[CarrierGridSiteIndex, EvowareLabwareModel]
): List[String] = {
	carrierIdInternal_l.toList.zipWithIndex.flatMap {
		case (None, _) => List("998;0;")
		case (Some(carrierId), gridIndex) =>
			val carrier = configFile.mapIdToCarrier(carrierId)
			//val sSiteCount = if (carrier.nSites > 0) carrier.nSites.toString else ""
			List(
				"998;"+carrier.nSites+";"+((0 until carrier.nSites).map(siteIndex => {
					val siteId = CarrierGridSiteIndex(carrierId, gridIndex, siteIndex)
					siteIdToLabwareModel_m2.get(siteId) match {
						case None => ""
						case Some(labwareModel) => labwareModel.sName
					}
				}).mkString(";"))+";",
				"998;"+((0 until carrier.nSites).map(siteIndex => {
					val siteId = CarrierGridSiteIndex(carrierId, gridIndex, siteIndex)
					siteIdToLabel_m2.get(siteId) match {
						case None => ""
						case Some(sLabel) => sLabel
					}
				}).mkString(";"))+";"
			)
	}
}

private def toString_hotels(): List[String] = {
	("998;"+hotelObject_l.length+";") :: hotelObject_l.map(o => "998;"+o.parent.id+";"+o.gridIndex+";")
}

private def toString_externals(): List[String] = {
	("998;"+externalObject_l.length+";") :: externalObject_l.map(o => "998;"+o.n1+";"+o.n2+";"+o.carrier.sName+";")
}

private def toString_externalLabware(
	siteIdToLabwareModel_m2: Map[CarrierGridSiteIndex, EvowareLabwareModel]
): List[String] = {
	// List of external carriers
	val carrierId_l: Set[Int] = externalObject_l.map(_.carrier.id).toSet
	val siteIdToLabwareModel_m3 = siteIdToLabwareModel_m2.filterKeys(siteId => carrierId_l.contains(siteId.carrierId))
	// Map of external carrier to labware model
	val carrierIdToLabwareModel_m: Map[Int, EvowareLabwareModel]
		= siteIdToLabwareModel_m3.toList.groupBy(_._1.carrierId).mapValues(l => l.head._2)
	// List of external carriers with labware on them
	val lCarrierToLabware = carrierIdToLabwareModel_m.toList.map { case (carrierId, labwareModelE) =>
		s"998;${carrierId};${labwareModelE.sName};"
	}
	("998;"+carrierIdToLabwareModel_m.size+";") :: lCarrierToLabware
}

private def toString_externalGrids(): List[String] = {
	externalObject_l.map(o => "998;"+carrierIdToGrids_m(o.carrier.id).head+";")
}
*/
