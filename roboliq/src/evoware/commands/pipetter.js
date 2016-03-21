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
	return handlePipetterSpirate(parsed, data);
}

export function _dispense(params, parsed, data) {
	return handlePipetterSpirate(parsed, data);
}

export function _pipette(params, parsed, data) {
	return handlePipetterSpirate(parsed, data);
}

export function _washTips(params, parsed, data) {
	//console.log("handleWashProgram: "+JSON.stringify(parsed, null, '\t'))

	function handleScript(filename) {
		return `Subroutine("${filename}",0);`;
	}

	function handleWashProgram(program) {
		const syringeRows = parsed.value.syringes.map(x => x.row);
		const syringeMask = encodeSyringesByRow(syringeRows);
		const bUNKNOWN1 = false;
		const lWash = [
			syringeMask,
			program.wasteGrid, program.wasteSite-1,
			program.cleanerGrid, program.cleanerSite-1,
			`"${math.format(program.wasteVolume, {precision: 14})}"`,
			program.wasteDelay,
			`"${math.format(program.cleanerVolume, {precision: 14})}"`,
			program.cleanerDelay,
			program.airgapVolume,
			program.airgapSpeed,
			program.retractSpeed,
			(program.fastWash) ? 1 : 0,
			(bUNKNOWN1) ? 1 : 0,
			1000,
			0
		];
		const lineWash = `Wash(${lWash.join(",")});`;

		const labwareModel = {rows: 8, columns: 1};
		const tuples = parsed.value.syringes.map(syringe => ({retract: {row: syringe.row, col: 1, labwareModel}}));
		const retractWellMask = encodeWells(tuples, "retract");
		// console.log({tuples: JSON.stringify(tuples), retractWellMask})
		const lRetract = [
			syringeMask,
			program.cleanerGrid, program.cleanerSite-1,
			1,
			`"${retractWellMask}"`,
			0, // 0=positioning with global z travel, 4=z-move
			4, // 4=global z travel
			0, 10, 0, 0
		];
		const lineRetract = `MoveLiha(${lRetract.join(",")});`;

		return [lineWash, lineRetract];
	}

	const program = (_.isString(parsed.value.program))
		? commandHelper.lookupPath([parsed.value.program], {}, data)
		: parsed.value.program;
	assert(!_.isEmpty(program), "missing wash program")
	//console.log({program})

	const results = [];
	if (!_.isEmpty(program.script)) {
		results.push({line: handleScript(program.script)});
	}
	else {
		handleWashProgram(program).forEach(line => results.push({line}));
	}
	return results;
}


function handlePipetterSpirate(parsed, data) {
	// Create groups of items that can be pipetted simultaneously
	const groups = groupItems(parsed, data);
	//console.log("groups:\n"+JSON.stringify(groups, null, '\t'));

	// Create a script line for each group:
	const results = _.flatMap(groups, group => handleGroup(parsed, data, group));
	//console.log("results:\n"+JSON.stringify(results, null, '\t'))
	if (groups.length > 0) {
		results.push(handleRetract(parsed, data, _.last(groups)))
	}

	// Get list of all accessed sites
	//	token_l2 <- handlePipetterSpirateDoGroup(objects, program, func, tuple_l.drop(tuple_l3.size))
	const siteToWellInfo = {};
	_.forEach(groups, group => _.forEach(group.tuples, tuple => {
		for (let propertyName of ["source", "destination"]) {
			const wellInfo = tuple[propertyName];
			if (wellInfo && !siteToWellInfo.hasOwnProperty(wellInfo.siteName))
				siteToWellInfo[wellInfo.siteName] = wellInfo;
		}
	}));
	//console.log({siteToWellInfo})
	const tableEffects = [];
	_.forEach(siteToWellInfo, (wellInfo, siteName) => {
		const key = [wellInfo.site.evowareCarrier, wellInfo.site.evowareGrid, wellInfo.site.evowareSite];
		const label = _.last(siteName.split("."));
		tableEffects.push([key, {label, labwareModelName: wellInfo.labwareModel.evowareName}]);
	});
	//console.log(tableEffects)

	return results.concat({tableEffects});
}

/**
 * Returns an array of groupings of the pipette items.  Each group has these properties:
 *
 * - `groupType` -- either "source" or "destination", depending on whether this group is for aspiration or dispense
 * - `tuples` -- an array with these properties:
 *     - `item` -- the original pipette item
 *     - `syringeName` -- roboliq name of the syringe to use
 *     - `source` -- source properties `{labwareName, labware, labwareModel, site, siteName, row, col}`
 *     - `destination` -- destination properties `{labwareName, labware, labwareModel, site, siteName, row, col}`
 * - `syringeSpacing`
 *
 * @param  {[type]} parsed [description]
 * @param  {[type]} data   [description]
 * @return {[type]}        [description]
 */
function groupItems(parsed, data) {
	if (_.isEmpty(parsed.value.items)) return [];

	//console.log("parsed:\n"+JSON.stringify(parsed, null, '\t'))
	const tuples = [];
	for (let i = 0; i < parsed.value.items.length; i++) {
		const item = parsed.value.items[i];
		const syringeName = parsed.objectName[`items.${i}.syringe`];
		//console.log("stuff: "+JSON.stringify(wellsParser.parseOne(item.source)))
		// const well = (item.hasOwnProperty("source")) ? item.source : item.destination;
		function getWellInfo(well) {
			if (_.isUndefined(well)) return undefined;
			const {labware: labwareName, wellId} = wellsParser.parseOne(well);
			//console.log({parseOne: wellsParser.parseOne(item.source)})
			//console.log({labwareName, wellId})
			const labware = commandHelper.lookupPath([labwareName], {}, data);
			const labwareModel = commandHelper.lookupPath([[labwareName, "model"]], {}, data);
			const [row, col] = wellsParser.locationTextToRowCol(wellId);
			const siteName = commandHelper.lookupPath([labwareName, "location"], {}, data);
			const site = commandHelper.lookupPath([[labwareName, "location"]], {}, data);
			return {labwareName, labware, labwareModel, site, siteName, row, col};
		}
		//labwareName <- ResultC.from(wellPosition.labware_?, "incomplete well specification; please also specify the labware")
		//labwareInfo <- getLabwareInfo(objects, labwareName)
		tuples.push({item, syringeName, source: getWellInfo(item.source), destination: getWellInfo(item.destination)});
	}
	//console.log("tuples:\n"+JSON.stringify(tuples, null, '\t'))

	//console.log({ref})
	// the spread of the syringes; normally this is 1, but Evoware can spread its syringes out more
	function canJoinGroup(group, tuple) {
		const ref = group.tuples[0];

		// Make sure the same syringe is not used twice in one group
		const isUniqueSyringe = _.every(group, tuple2 => tuple2.syringeName != tuple.syringeName);
		if (!isUniqueSyringe)
			return false;

		function checkWellInfo(wellInfo, wellInfoRef) {
			// Same column?
			if (wellInfo.col === wellInfoRef.col) {
				//console.log({tuple})
				const dRow1 = tuple.item.syringe.row - ref.item.syringe.row
				const dRow2 = wellInfo.row - wellInfoRef.row;
				//console.log({tupleSyringe: tuple.item.syringe, refSyringe: ref.item.syringe})
				if (_.isUndefined(group.syringeSpacing)) {
					if (dRow1 === 0 || dRow2 === 0) {
						return false;
					}
					//console.log(1)
					//console.log({dRow1, dRow2})
					const syringeSpacing = math.fraction(dRow2, dRow1);
					// console.log(2)
					// FIXME: need to check wether the syringe spacing is permissible!  Check how much the syringes need to spread physically (not just relative to the plate wells), and whether that's possible for the hardware.  Also, not all fractions will be permissible, probably.
					if (syringeSpacing < 1) {
						return false;
					}
					else {
						group.syringeSpacing = syringeSpacing;
						return true;
					}
				}
				else {
					// console.log(3)
					if (math.equal(math.fraction(dRow2, dRow1), syringeSpacing))
						return true;
					// console.log(4)
				}
			}
			return false;
		}
		// Same labware?
		if ((tuple.source && tuple.source.labwareName !== ref.source.labwareName) || (tuple.destination && tuple.destination.labwareName !== ref.destination.labwareName))
			return false;
		// Other things ok?
		if (!_.isUndefined(tuple[group.groupType])) {
			if (checkWellInfo(tuple[group.groupType], ref[group.groupType]))
				return true;
		}

		return false;
	}

	let groupSrc = {groupType: "source", tuples: [tuples[0]]};
	let groupDst = {groupType: "destination", tuples: [tuples[0]]};
	const groups = [groupSrc, groupDst];
	for (let i = 1; i < tuples.length; i++) {
		const tuple = tuples[i];
		// Can the source can be added to the source group?
		if (canJoinGroup(groupSrc, tuple)) {
			groupSrc.tuples.push(tuple);
			// Decide whether to add to the current destination group or to create new one
			if (canJoinGroup(groupDst, tuple)) {
				groupDst.tuples.push(tuple);
			}
			else {
				groupDst = {groupType: "destination", tuples: [tuple]};
				groupDst.push(groupDst);
			}
		}
		// No, so start new src and dst groups
		else {
			// Start a new group`
			groupSrc = {groupType: "source", tuples: [tuple]};
			groupDst = {groupType: "destination", tuples: [tuple]};
			groupSrc.push(groupSrc);
			groupDst.push(groupDst);
		}
	}
	groupSrc.syringeSpacing = groupSrc.syringeSpacing || 1;
	groupDst.syringeSpacing = groupDst.syringeSpacing || 1;

	return groups;
}

/**
 * [handlePipetterSpirateHandleGroup description]
 * @param  {object} objects - current object state
 * @param  {string} program - name of the evoware liquid class
 * @param  {string} func - "Aspirate" or "Dispense"
 * @param  {array} tuple_l - List[(PipetterItem, WellNameSingleParsed, Any)]
 * @param  {?} labwareInfo - LabwareInfo
 * @param  {integer} tipSpacing - how far apart the tips should be (the base value 1)
 * @return {array} array of line info
 */
function handleGroup(parsed, data, group) {
	assert(group.tuples.length > 0);

	const tuples = group.tuples;
	// Calculate syringe mask
	const tuple0 = tuples[0];
	const syringeMask = encodeSyringes(tuples);
	// Volumes for each syringe (in ul)
	const volumes = _.fill(Array(12), "0");
	_.forEach(tuples, tuple => {
		const index = (tuple.item.syringe.row || 1) - 1;
		const ul = tuple.item.volume.toNumber('ul');
		volumes[index] = `"${math.format(ul, {precision: 14})}"`;
	});

	// Script command line
	function makeLines(func, propertyName) {
		const wellInfo = tuple0[propertyName];
		// console.log({func, propertyName, wellInfo})
		if (_.isUndefined(wellInfo))
			return [];

		const labwareModel = wellInfo.labwareModel;
		const plateMask = encodeWells(tuples, propertyName);
		const l = [
			syringeMask,
			`"${stripQuotes(parsed.value.program)}"`,
			volumes.join(","),
			wellInfo.site.evowareGrid, wellInfo.site.evowareSite - 1,
			group.syringeSpacing,
			`"${plateMask}"`,
			0,
			0
		];
		const line = `${func}(${l.join(",")});`;
		return [{line}];
	}

	const func = (group.groupType === "source") ? "Aspirate" : "Dispense";
	//console.log({syringeMask, volumes})
	return makeLines(func, group.groupType);
}

function handleRetract(parsed, data, group) {
	assert(group.tuples.length > 0);

	const tuples = group.tuples;
	// Calculate syringe mask
	const tuple0 = _.last(tuples);
	const syringeMask = encodeSyringes(tuples);

	// Script command line
	const propertyName = (tuple0.destination) ? "destination" : "source";
	const wellInfo = tuple0[propertyName];
	// console.log({propertyName, wellInfo, tuple0})
	if (_.isUndefined(wellInfo))
		return undefined;

	const labwareModel = wellInfo.labwareModel;
	const plateMask = encodeWells(tuples, propertyName);
	const l = [
		syringeMask,
		wellInfo.site.evowareGrid, wellInfo.site.evowareSite - 1,
		1,
		`"${plateMask}"`,
		0, // 0=positioning with global z travel, 4=z-move
		4, // 4=global z travel
		0, 10, 0, 0
	];
	const line = `MoveLiha(${l.join(",")});`;
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
 * Generate a bitmap encoding of syringes to use
 * @param  {array} rows - array of syringe rows (base value is 1)
 * @return {integer} an bitmask encoding of the syringes
 */
function encodeSyringesByRow(rows) {
	return _.sum(_.map(rows, row => 1 << (row - 1)));
}

/**
 * Encode a list of wells on a plate as an evoware bitmask
 */
//function encodeWells(rows, cols, well_l: Traversable[WellNameSingleParsed]): ResultC[String] = {
function encodeWells(tuples, propertyName) {
	assert(tuples.length > 0);
	const labwareModel = tuples[0][propertyName].labwareModel;
	const nWellMaskChars = math.ceil(labwareModel.rows * labwareModel.columns / 7.0);
	const amWells = _.fill(Array(nWellMaskChars), 0);
	_.forEach(tuples, tuple => {
		const index = (tuple[propertyName].row - 1) + (tuple[propertyName].col - 1) * labwareModel.rows;
		const iChar = _.toInteger(index / 7);
		const iWell1 = index % 7;
		assert(iChar < amWells.length, "INTERNAL ERROR: encodeWells: index out of bounds -- "+JSON.stringify(tuple));
		// console.log({index, iChar, iWell1})
		amWells[iChar] += 1 << iWell1;
	});
	// console.log({amWells})
	const sWellMask = amWells.map(EvowareUtils.encode).join("");
	const sPlateMask = sprintf("%02X%02X", labwareModel.columns, labwareModel.rows) + sWellMask;
	return sPlateMask;
}
