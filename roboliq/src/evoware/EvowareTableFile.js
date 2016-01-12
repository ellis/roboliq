import _ from 'lodash';
import assert from 'assert';
import fs from 'fs';
import iconv from 'iconv-lite';
import lineByLine from 'n-readlines';
import {sprintf} from 'sprintf-js';
import EvowareUtils from './EvowareUtils.js';

/*
val configFile: EvowareCarrierData,
val carrierIdInternal_l: scala.collection.immutable.Vector[Option[Int]],
//lCarrier_? : List[Option[Carrier]],
val hotelObject_l: List[HotelObject],
val externalObject_l: List[ExternalObject],
val carrierIdToGrids_m: Map[Int, List[Int]],
//val mapCarrierToGrid: Map[Carrier, Int],
val siteIdToLabel_m: Map[CarrierGridSiteIndex, String],
val siteIdToLabwareModel_m: Map[CarrierGridSiteIndex, EvowareLabwareModel]

 */
/*
 * Represents the table setup for an Evoware script file.
 */

/**
 * Represents the table setup for an Evoware script file.
 * @param {EvowareCarrierData} carrierData
 * @param {array} carrierIdsInternal - List with optional CarrierID for each grid on the table
 * @param {array} hotelObjects - array of HotelObjects
 * @param {array} externalObjects - array of ExternalObjects
 * @param {object} carrierIdToGrids - map from carrierId to grid indexes where that carrier is used
 * @param {object} siteIdToLabel - map from CarrierGridSiteIndex to name of the site
 * @param {object} siteIdTo CONTINUE
 */

CONTINUE: think about siteIdToLabel above, because the key is not a string!
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
