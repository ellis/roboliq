import _ from 'lodash';
import assert from 'assert';
import fs from 'fs';
import iconv from 'iconv-lite';
import lineByLine from 'n-readlines';
import {sprintf} from 'sprintf-js';
import EvowareUtils from './EvowareUtils.js';

/**
 * Represents the table setup for an Evoware script file.
 * @param {EvowareCarrierData} carrierData
 * @param {array} carrierIdsInternal - List with optional CarrierID for each grid on the table
 * @param {array} hotelObjects - array of HotelObjects
 * @param {array} externalObjects - array of ExternalObjects
 * @param {object} carrierIdToGrids - map from carrierId to grid indexes where that carrier is used
 * @param {object} siteIdToLabel - map from carrierIndex to gridIndex to siteIndex to name of the site
 * @param {object} siteIdToLabwareModel - map from carrierIndex to gridIndex to siteIndex to labware model at the site
 */
export class EvowareTableData {
	constructor(carrierData, carrierIdsInternal, hotelObjects, externalObjects, carrierIdToGrids, siteIdToLabel, siteIdToLabwareModel) {
		this.carrierData = carrierData;
		this.carrierIdsInternal = carrierIdsInternal;
		this.hotelObjects = hotelObjects;
		this.externalObjects = externalObjects;
		this.carrierIdToGrids = carrierIdToGrids;
		this.siteIdToLabel = siteIdToLabel;
		this.siteIdToLabwareModel = siteIdToLabwareModel;
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
 * @param {integer} carrierId - carrier ID for the carrier holding this hotel
 */
export class ExternalObject {
	constructor(n1, n2, carrierId) {
		this.n1 = n1;
		this.n2 = n2;
		this.carrierId = carrierId;
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
	lines.skip(2);
	const [lineIndex2, tableFile] = parse14(carrierData, l, lines);
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
	const gridToCarrierId = parse14_getCarriers(_.initial(l))
	const gridToCarrier =
	const carrierIds = _.filter(gridToCarrierId, n => n > -1);
	const carriers = carrierIds.map(id => carrierData.idToCarrier[id]);
	const lCarrier_? : List[Option[Carrier]] = carrierId_l.map(carrierId_? => carrierId_?.flatMap(carrierId => configFile.mapIdToCarrier.get(carrierId)))
	const gridToInternalCarrierId_m: Map[Int, Int]
		= carrierId_l.zipWithIndex.collect({case (Some(carrierId), gridIndex) => gridIndex -> carrierId}).toMap
	const lTableInfo = parse14_getLabwareObjects(carrierData, gridToCarrierId, lines);
	const (lHotelObject, lsLine3) = parse14_getHotelObjects(mapIdToCarrier, lsLine2)
	const (lExternalObject, lsLine4) = parse14_getExternalObjects(mapNameToCarrier, lsLine3)
	const (mapSiteToExternalLabwareModel, lsLine5) = parse14_getExternalLabwares(mapIdToCarrier, mapNameToLabwareModel, lsLine4)
	const (mapCarrierToGrid2, lsLine6) = parse14_getExternalCarrierGrids(lExternalObject, lsLine5)

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

	logger.trace("tablefile:")
	logger.trace(tableFile.toDebugString)
	(tableFile, lsLine6)
}

/**
 * Extract array where the array index is the grid index and the value is the carrier ID.
 * This information is on the first line of the table definition.
 * A -1 value for the carrier ID means that there is no carrier at that grid.
 * @param  {array} l - elements of line
 * @return {array} array of carrier IDs on this table
 */
function parse14_getCarriers(l) {
	return l.map(s => parseInt(s));
}

/**
 * Get array of labwares on the table.
 * @param  {EvowareCarrierData} carrierData
 * @param  {EvowareSemicolonFile} lines - lines of table file
 * @return {array} an array of tuples (CarrierGridSiteIndex, site label, EvowareLabwareModel)
 */
function parse14_getLabwareObjects(carrierData, gridToCarrierId, lines) {
	const result = [];
	gridToCarrierId.forEach((carrierId, gridIndex) => {
		if (carrierId > -1) {
			const carrier = carrierData.idToCarrier[carrierId];
			const [n0, l0] = lines.nextSplit();
			const [n1, l1] = lines.nextSplit();
			assert(n0 == 998 && n1 == 998 && parseInt(l0[0]) === carrier.siteCount);
			//println(iGrid+": "+carrier)
			CONTINUE
			const l = _.compact(_.range(0, ))(for (iSite <- 0 until carrier.siteCount) yield {
				//println("\t"+i+": "+l0(i+1)+", "+l1(i))
				const sName = l0(iSite+1)
				if (sName.isEmpty()) None
				else Some(CarrierGridSiteIndex(carrier.id, gridIndex, iSite), l1(iSite), mapNameToLabwareModel(sName))
			}).toList.flatten
			result.push.apply(result, l);
		}
		else {
			lines.skip(1);
		}
	});
	lCarrier_? match {
		case Nil => (acc, lsLine)
		case None :: rest => parse14_getLabwareObjects(mapNameToLabwareModel, gridIndex + 1, rest, lsLine.tail, acc)
		case Some(carrier) :: rest =>
	}
}

/**
 * Parse the hotel objects
 * @param  {[type]} mapIdToCarrier: Map[Int       [description]
 * @param  {[type]} Carrier]        [description]
 * @param  {[type]} lsLine:         List[String]  [description]
 * @return {[type]}                 [description]
 */
function parse14_getHotelObjects(
	mapIdToCarrier: Map[Int, Carrier],
	lsLine: List[String]
): Tuple2[List[HotelObject], List[String]] = {
	const (n0, l0) = EvowareUtils.splitSemicolons(lsLine(0))
	assert(n0 == 998)
	const nHotels = l0(0).toInt
	const lHotelObject = lsLine.tail.take(nHotels).map(s => {
		const (n, l) = EvowareUtils.splitSemicolons(s)
		assert(n == 998)
		const id = l(0).toInt
		const iGrid = l(1).toInt
		const parent = mapIdToCarrier(id)
		HotelObject(parent, iGrid)
	})
	(lHotelObject, lsLine.drop(1 + nHotels))
}

def parse14_getExternalObjects(
	mapNameToCarrier: Map[String, Carrier],
	lsLine: List[String]
): Tuple2[List[ExternalObject], List[String]] = {
	const (n0, l0) = EvowareUtils.splitSemicolons(lsLine(0))
	assert(n0 == 998)
	const nObjects = l0(0).toInt
	const lObject = lsLine.tail.take(nObjects).map(s => {
		const (n, l) = EvowareUtils.splitSemicolons(s)
		assert(n == 998)
		const n1 = l(0).toInt
		const n2 = l(1).toInt
		const sName = l(2)
		const carrier =
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
	const (n0, l0) = EvowareUtils.splitSemicolons(lsLine(0))
	assert(n0 == 998)
	const nObjects = l0(0).toInt
	const mapSiteToLabwareModel = lsLine.tail.take(nObjects).map(s => {
		const (n, l) = EvowareUtils.splitSemicolons(s)
		assert(n == 998)
		const carrierId = l(0).toInt
		const sName = l(1)
		const carrier = mapIdToCarrier(carrierId)
		const labwareModel = mapNameToLabwareModel(sName)
		CarrierSite(carrier, 0) -> labwareModel
	}).toMap
	(mapSiteToLabwareModel, lsLine.drop(1 + nObjects))
}

def parse14_getExternalCarrierGrids(
	lExternalObject: List[ExternalObject],
	lsLine: List[String]
): Tuple2[Map[Carrier, Int], List[String]] = {
	const map = (lExternalObject zip lsLine).map(pair => {
		const (external, sLine) = pair
		const (n, l) = EvowareUtils.splitSemicolons(sLine)
		assert(n == 998)
		// We need to force the system liquid to be on grid -1
		const iGrid = if (external.carrier.id == -1) -1 else l(0).toInt
		external.carrier -> iGrid
	}).toMap
	(map, lsLine.drop(lExternalObject.length))
}
