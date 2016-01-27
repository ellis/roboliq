import _ from 'lodash';
import math from 'mathjs';
import {sprintf} from 'sprintf-js';
import commandHelper from '../../commandHelper.js';
import evowareHelper from './evowareHelper.js';
import EvowareUtils from '../EvowareUtils.js';
import wellsParser from '../../parsers/wellsParser.js';

function stripQuotes(s) {
	return (_.startsWith(s, '"') && _.endsWith(s, '"'))
		? s.substring(1, s.length - 1) : s;
}

export function _aspirate(params, parsed, data) {
	return handlePipetterSpirate(parsed, data, "Aspirate");
}

function handlePipetterSpirate(parsed, data, func) {
	const groups = groupItems(parsed, data);
	console.log("groups:\n"+JSON.stringify(groups));
	//	token_l2 <- handlePipetterSpirateDoGroup(objects, program, func, tuple_l.drop(tuple_l3.size))
	return {};
}

function groupItems(parsed, data) {
	if (_.isEmpty(parsed.value.items)) return [];

	console.log("parsed:\n"+JSON.stringify(parsed, null, '\t'))
	const tuples = _.map(parsed.value.items, item => {
		//console.log("stuff: "+JSON.stringify(wellsParser.parseOne(item.source)))
		const {labware: labwareName, wellId} = wellsParser.parseOne(item.source);
		console.log({parseOne: wellsParser.parseOne(item.source)})
		console.log({labwareName, wellId})
		const labware = commandHelper.lookupPath([labwareName], {}, data);
		const labwareModel = commandHelper.lookupPath([labwareName, "model"], {}, data);
		const [row, col] = wellsParser.locationTextToRowCol(wellId);
		//labwareName <- ResultC.from(wellPosition.labware_?, "incomplete well specification; please also specify the labware")
		//labwareInfo <- getLabwareInfo(objects, labwareName)
		return {item, labwareName, labware, labwareModel, row, col};
	});
	console.log("tuples:\n"+JSON.stringify(tuples, null, '\t'))

	let ref = tuples[0];
	let syringeSpacing; // the spread of the syringes; normally this is 1, but Evoware can spread its syringes out more
	function canJoinGroup(tuple) {
		// Same labware?
		if (tuple.labwareName === ref.labwareName) {
			// FIXME: need to accomodate 2D LiHa's by allowing for columns differences too
			// Same column?
			if (tuple.col === ref.col) {
				const dRow1 = tuple.item.syringe.row - ref.item.syringe.row
				const dRow2 = tuple.row - ref.row;
				if (_.isUndefined(syringeSpacing)) {
					syringeSpacing = math.fraction(dRow2, dRow1);
					// FIXME: need to check wether the syringe spacing is permissible!  Check how much the syringes need to spread physically (not just relative to the plate wells), and whether that's possible for the hardware.  Also, not all fractions will be permissible, probably.
					if (syringeSpacing < 1) {
						return false;
					}
					else {
						return true;
					}
				}
				else {
					if (math.equal(math.fraction(dRow2, dRow1), syringeSpacing))
						return true;
				}
			}
		}
		return false;
	}

	let group = [ref];
	const groups = [group];
	for (let i = 1; i < tuples.length; i++) {
		const tuple = tuples[i];
		if (canJoinGroup(tuple)) {
			group.push(tuple);
		}
		else {
			ref = tuple;
			group = [ref];
			groups.push(group);
		}
	}

	return groups;
}

/**
 * [handlePipetterSpirateHandleGroup description]
 * @param  {object} objects - current object state
 * @param  {string} program - name of the evoware liquid class
 * @param  {string} func - "Aspirate" or "Dispense"
 * @param  {array} tuple_l - List[(PipetterItem, WellNameSingleParsed, Any)]
 * @param  {?} labwareInfo - LabwareInfo
 * @param  {integer} tipSpacing - how far apart the tips should be (FIXME: is the base value 1 or 0?)
 * @return {array} array of line info
 */
function handleGroup(parsed, data, func, tuples) {
	assert(tuples.length > 0);

	// Calculate syringe mask
	const syringeMask = encodeSyringes(tuples);

	val well_l = tuple_l.map(_._2)
	const volumes = _.fill(Array(12), "0");

	const labwareModel = tuples[0].labwareModel;
	const plateMask = encodeWells(tuples);
	for {
		labwareModelInfo <- getLabwareModelInfo(objects, labwareInfo.labwareModelName0)
		plateMask <- encodeWells(labwareModelInfo.rowCount, labwareModelInfo.colCount, well_l)
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		_ <- ResultC.foreach(tuple_l) { tuple =>
			for {
				amount <- AmountParser.parse(tuple._1.volume)
				syringe = tuple._1.syringe
				_ <- ResultC.assert(syringe >= 1 && syringe <= 12, `invalid syringe value ${syringe}: must be between 1 and 12`)
				volumeString <- amount match {
					case Amount_Volume(volume) =>
						ResultC.unit(df.format(volume.ul))
					case Amount_Variable(name) =>
						ResultC.unit(name)
					case _ =>
						ResultC.error(s"invalid volume `${tuple._1.volume}`: expected liquid volume or evoware variable name, but got $amount")
				}
			} yield {
				volume_l(syringe - 1) = s""""$volumeString""""
			}
		}
	} yield {
		const l = [
			syringeMask,
			`"${program}"`,
			volume_l.join(","),
			labwareInfo.cngs.gridIndex, labwareInfo.cngs.siteIndex,
			tipSpacing,
			`"${plateMask}"`,
			0,
			0
		];
		const line = `${func}(${l.join(",")});`;

		val siteToNameAndModel_m: Map[CarrierNameGridSiteIndex, (String, String)] = {
			// Don't set labware for the "System" liquid site
			if (labwareInfo.cngs.carrierName == "System") Map()
			else Map(labwareInfo.cngs -> (labwareInfo.siteName, labwareInfo.labwareModelName))
		}

		List(Token(line, JsObject(), siteToNameAndModel_m))
	}
}

/**
 * Generate a bitmap encoding of syringes to use
 * @param  {array} syringes - array of syringes to use
 * @return {integer} an bitmask encoding of the syringes
 */
function encodeSyringes(tuples) {
	return _.sum(_.map(tuples, tuple => 1 << (tuple.item.syringe.row - 1)));
}

/**
 * Encode a list of wells on a plate as an evoware bitmask
 */
//function encodeWells(rows, cols, well_l: Traversable[WellNameSingleParsed]): ResultC[String] = {
function encodeWells(tuples) {
	assert(tuples.length > 0);
	const labwareModel = tuples[0].labwareModel;
	//println("encodeWells:", holder.nRows, holder.nCols, aiWells)
	const nWellMaskChars = math.ceil(labwareModel.rows * labwareModel.columns / 7.0);
	const amWells = _.fill(Array(nWellMaskChars), 0);
	_.forEach(tuples, tuple => {
		const index = tuple.row + tuple.col * labwareModel.rows;
		const iChar = _.toInteger(index / 7);
		const iWell1 = index % 7;
		assert(iChar < amWells.length, "INTERNAL ERROR: encodeWells: index out of bounds -- "+[rows, cols, well, index, iChar, iWell1, well_l]);
		amWells[iChar] += 1 << iWell1;
	});
	const sWellMask = amWells.map(EvowareUtils.encode).join();
	const sPlateMask = sprintf("%02d%02d", labwareModel.columns, labwareModel.rows) + sWellMask;
	return sPlateMask;
}
