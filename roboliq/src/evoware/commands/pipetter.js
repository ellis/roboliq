import _ from 'lodash';
import assert from 'assert';
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
	// Create groups of items that can be pipetted simultaneously
	const groups = groupItems(parsed, data);
	//console.log("groups:\n"+JSON.stringify(groups));

	// Create a script line for each group:
	const results = groups.map(group => handleGroup(parsed, data, func, group));
	//console.log("results:\n"+JSON.stringify(results, null, '\t'))

	// Get list of all accessed sites
	//	token_l2 <- handlePipetterSpirateDoGroup(objects, program, func, tuple_l.drop(tuple_l3.size))
	const siteToTuple = {};
	_.forEach(groups, group => _.forEach(group.tuples, tuple => {
		//const key = [tuple.site.evowareCarrier, tuple.site.evowareGrid, tuple.site.evowareSite];
		if (!siteToTuple.hasOwnProperty(tuple.siteName))
			siteToTuple[tuple.siteName] = tuple;
	}));
	const tableEffects = [];
	_.forEach(siteToTuple, (tuple, siteName) => {
		const key = [tuple.site.evowareCarrier, tuple.site.evowareGrid, tuple.site.evowareSite];
		const label = _.last(siteName.split("."));
		tableEffects.push([key, {label, labwareModelName: tuple.labwareModel.evowareName}]);
	});
	//console.log(tableEffects)

	return results.concat({tableEffects});
}

function groupItems(parsed, data) {
	if (_.isEmpty(parsed.value.items)) return [];

	console.log("parsed:\n"+JSON.stringify(parsed, null, '\t'))
	const tuples = [];
	for (let i = 0; i < parsed.value.items.length; i++) {
		const item = parsed.value.items[i];
		//console.log("stuff: "+JSON.stringify(wellsParser.parseOne(item.source)))
		const {labware: labwareName, wellId} = wellsParser.parseOne(item.source);
		//console.log({parseOne: wellsParser.parseOne(item.source)})
		//console.log({labwareName, wellId})
		const labware = commandHelper.lookupPath([labwareName], {}, data);
		const labwareModel = commandHelper.lookupPath([[labwareName, "model"]], {}, data);
		const [row, col] = wellsParser.locationTextToRowCol(wellId);
		const siteName = commandHelper.lookupPath([labwareName, "location"], {}, data);
		const site = commandHelper.lookupPath([[labwareName, "location"]], {}, data);
		const syringeName = parsed.objectName[`items.${i}.syringe`];
		//labwareName <- ResultC.from(wellPosition.labware_?, "incomplete well specification; please also specify the labware")
		//labwareInfo <- getLabwareInfo(objects, labwareName)
		tuples.push({item, labwareName, labware, labwareModel, site, siteName, row, col, syringeName});
	}
	console.log("tuples:\n"+JSON.stringify(tuples, null, '\t'))

	let ref = tuples[0];
	// the spread of the syringes; normally this is 1, but Evoware can spread its syringes out more
	let syringeSpacing;
	function canJoinGroup(group, tuple) {
		// Make sure the same syringe is not used twice in one group
		// FIXME: perform a better comparison -- need to compare the names instead of rows, because we might have multiple LiHas or multiple columns
		const isUniqueSyringe = _.every(group, tuple2 => tuple2.item.syringe.row != tuple.item.syringe.row);
		// Same labware?
		if (tuple.labwareName === ref.labwareName && isUniqueSyringe) {
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

	let group = {tuples: [ref]};
	const groups = [group];
	for (let i = 1; i < tuples.length; i++) {
		const tuple = tuples[i];
		if (canJoinGroup(group, tuple)) {
			group.push(tuple);
		}
		else {
			group.syringeSpacing = syringeSpacing || 1;
			// Start a new group`
			ref = tuple;
			group = {tuples: [ref]};
			groups.push(group);
			syringeSpacing = undefined;
		}
	}
	group.syringeSpacing = syringeSpacing || 1;

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
function handleGroup(parsed, data, func, group) {
	assert(group.tuples.length > 0);

	const tuples = group.tuples;
	// Calculate syringe mask
	const tuple0 = tuples[0];
	const syringeMask = encodeSyringes(tuples);
	const labwareModel = tuple0.labwareModel;
	const plateMask = encodeWells(tuples);
	// Volumes for each syringe (in ul)
	const volumes = _.fill(Array(12), "0");
	_.forEach(tuples, tuple => {
		const index = (tuple.item.syringe.row || 1) - 0;
		const ul = tuple.item.volume.toNumber('ul');
		volumes[index] = `"${math.format(ul, {precision: 14})}"`;
	});

	// Script command line
	const l = [
		syringeMask,
		`"${stripQuotes(parsed.value.program)}"`,
		volumes.join(","),
		tuple0.site.evowareGrid, tuple0.site.evowareSite - 1,
		group.syringeSpacing,
		`"${plateMask}"`,
		0,
		0
	];
	const line = `${func}(${l.join(",")});`;
	return {line};
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
		assert(iChar < amWells.length, "INTERNAL ERROR: encodeWells: index out of bounds -- "+JSON.stringify(tuple));
		amWells[iChar] += 1 << iWell1;
	});
	const sWellMask = amWells.map(EvowareUtils.encode).join("");
	const sPlateMask = sprintf("%02X%02X", labwareModel.columns, labwareModel.rows) + sWellMask;
	return sPlateMask;
}
