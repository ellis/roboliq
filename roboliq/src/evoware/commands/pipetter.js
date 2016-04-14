import _ from 'lodash';
import assert from 'assert';
import math from 'mathjs';
import {sprintf} from 'sprintf-js';
import commandHelper from '../../commandHelper.js';
import evowareHelper from './evowareHelper.js';
import EvowareUtils from '../EvowareUtils.js';
import * as Tokens from './tokens.js';
import wellsParser from '../../parsers/wellsParser.js';

export function _aspirate(params, parsed, data) {
	return handlePipetterSpirate(parsed, data);
}

export function _dispense(params, parsed, data) {
	return handlePipetterSpirate(parsed, data);
}

export function _mix(params, parsed, data) {
	return handlePipetterSpirate(parsed, data, {well: "Mix"});
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
		const syringeRows = parsed.value.syringes.map(x => _.isNumber(x) ? x : x.row);
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

		const doRetract = _.get(data, ["protocol", "config", "evowareCompiler", "retractTips"], true);
		if (doRetract) {
			const labwareModel = {rows: 8, columns: 1};
			const tuples = parsed.value.syringes.map(syringe => ({retract: {row: _.isNumber(syringe) ? syringe : syringe.row, col: 1, labwareModel}}));
			const retractWellMask = encodeWells(tuples, "retract");
			// console.log({tuples: JSON.stringify(tuples), retractWellMask})
			const lRetract = [
				syringeMask,
				program.cleanerGrid, program.cleanerSite-1,
				1, // tip spacing
				`"${retractWellMask}"`,
				4, // 0=positioning with global z travel, 4=z-move
				4, // 4=global z travel
				0,
				400, // speed (mm/s), min 0.1, max 400
				0, 0
			];
			const lineRetract = `MoveLiha(${lRetract.join(",")});`;

			return [lineWash, lineRetract];
		}
		else {
			return [lineWash];
		}
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


function handlePipetterSpirate(parsed, data, groupTypeToFunc) {
	if (!groupTypeToFunc) {
		groupTypeToFunc = {
			"source": "Aspirate",
			"destination": "Dispense"
		};
	}

	// Create groups of items that can be pipetted simultaneously
	const groups = groupItems(parsed, data);
	// console.log("groups:\n"+JSON.stringify(groups, null, '\t'));

	// Create a script line for each group:
	const results = _.flatMap(groups, group => handleGroup(parsed, data, group, groupTypeToFunc));
	// console.log("results:\n"+JSON.stringify(results, null, '\t'))
	const doRetract = _.get(data, ["protocol", "config", "evowareCompiler", "retractTips"], true);
	if (groups.length > 0 && doRetract) {
		results.push(handleRetract(parsed, data, groups))
	}

	// Get list of all accessed sites
	//	token_l2 <- handlePipetterSpirateDoGroup(objects, program, func, tuple_l.drop(tuple_l3.size))
	const siteToWellInfo = {};
	_.forEach(groups, group => _.forEach(group.tuples, tuple => {
		for (let propertyName of ["source", "destination", "well"]) {
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
 *     - `syringe` -- syringe object
 *     - `syringeRow` -- row of syringe on the LiHa
 *     - `source` -- source properties `{labwareName, labware, labwareModel, site, siteName, row, col}`
 *     - `destination` -- destination properties `{labwareName, labware, labwareModel, site, siteName, row, col}`
 * - `syringeSpacing`
 *
 * @param  {[type]} parsed [description]
 * @param  {[type]} data   [description]
 * @return {[type]}        [description]
 */
function groupItems(parsed, data) {
	//console.log("parsed:\n"+JSON.stringify(parsed, null, '\t'))

	let items = commandHelper.copyItemsWithDefaults(parsed.value.items, parsed.value.itemDefaults);
	// console.log("groupItems items:"+JSON.stringify(items))
	if (_.isEmpty(items)) return [];

	const tuples = [];
	for (let i = 0; i < items.length; i++) {
		const item = items[i];
		const syringeName = _.isInteger(item.syringe)
			? `${parsed.objectName.equipment}.syringe.${item.syringe}`
			: parsed.objectName[`items.${i}.syringe`];
		const syringe = commandHelper._g(data, syringeName);
		const syringeRow = _.isInteger(item.syringe)
			? item.syringe
			: syringe.row;
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
		tuples.push({item, syringeName, syringeRow, source: getWellInfo(item.source), destination: getWellInfo(item.destination), well: getWellInfo(item.well)});
	}
	// console.log("tuples:\n"+JSON.stringify(tuples, null, '\t'))

	//console.log({ref})
	// the spread of the syringes; normally this is 1, but Evoware can spread its syringes out more
	function canJoinGroup(group, tuple, debug) {
		// console.log("canJoinGroup: "+JSON.stringify({group, tuple}, null, '\t'))
		const ref = group.tuples[0];

		// Make sure the same syringe is not used twice in one group
		const isUniqueSyringe = _.every(group.tuples, tuple2 => tuple2.syringeName != tuple.syringeName);
		if (!isUniqueSyringe) {
			if (debug) console.log({group, tuple, isUniqueSyringe, syringes: group.tuples.map(x => x.syringeName)})
			return false;
		}

		function checkWellInfo(wellInfo, wellInfoRef) {
			// Same column?
			if (wellInfo.col === wellInfoRef.col) {
				//console.log({tuple})
				const dRow1 = tuple.syringeRow - ref.syringeRow
				const dRow2 = wellInfo.row - wellInfoRef.row;
				//console.log({tupleSyringe: tuple.item.syringe, refSyringe: ref.item.syringe})
				if (_.isUndefined(group.syringeSpacing)) {
					if (dRow1 === 0 || dRow2 === 0) {
						if (debug) console.log({group, tuple, dRow1, dRow2})
						return false;
					}
					//console.log(1)
					//console.log({dRow1, dRow2})
					const syringeSpacing = math.fraction(dRow2, dRow1);
					// console.log(2)
					// FIXME: need to check wether the syringe spacing is permissible!  Check how much the syringes need to spread physically (not just relative to the plate wells), and whether that's possible for the hardware.  Also, not all fractions will be permissible, probably.
					if (syringeSpacing < 1) {
						if (debug) console.log({group, tuple, syringeSpacing})
						return false;
					}
					else {
						group.syringeSpacing = syringeSpacing;
						return true;
					}
				}
				else {
					// console.log(3)
					if (math.equal(math.fraction(dRow2, dRow1), group.syringeSpacing))
						return true;
					// console.log(4)
				}
			}
			if (debug) console.log({group, tuple, col: wellInfo.col, colRef: wellInfoRef.col})
			return false;
		}
		// Same labware?
		if ((tuple.source && tuple.source.labwareName !== ref.source.labwareName) || (tuple.destination && tuple.destination.labwareName !== ref.destination.labwareName) || (tuple.well && tuple.well.labwareName !== ref.well.labwareName)) {
			if (debug) console.log({group, tuple, col: wellInfo.col, colRef: wellInfoRef.col})
			return false;
		}
		// Other things ok?
		if (!_.isUndefined(tuple[group.groupType])) {
			if (checkWellInfo(tuple[group.groupType], ref[group.groupType]))
				return true;
		}

		return false;
	}

	let groupSrc = {groupType: "source", tuples: []};
	let groupDst = {groupType: "destination", tuples: []};
	let groupWll = {groupType: "well", tuples: []};
	const groups = [groupSrc, groupDst, groupWll];
	const debug = false;
	for (let i = 0; i < tuples.length; i++) {
		const tuple = tuples[i];

		let needNew = false;
		if (tuple.well) {
			if (groupWll.tuples.length === 0 || canJoinGroup(groupWll, tuple, debug)) {
				groupWll.tuples.push(tuple);
			}
			else {
				needNew = true;
			}
		}
		else {
			if (tuple.source) {
				if (groupSrc.tuples.length === 0 || canJoinGroup(groupSrc, tuple, debug)) {
					groupSrc.tuples.push(tuple);
				}
				else {
					needNew = true;
				}
			}
			if (!needNew && tuple.destination) {
				if (groupDst.tuples.length === 0 || canJoinGroup(groupDst, tuple, debug)) {
					groupDst.tuples.push(tuple);
				}
				else {
					groupDst = {groupType: "destination", tuples: [tuple]};
					groups.push(groupDst);
				}
			}
		}

		// No, so start new src and dst groups
		if (needNew) {
			// Start a new group`
			groupSrc = {groupType: "source", tuples: []};
			groupDst = {groupType: "destination", tuples: []};
			groupWll = {groupType: "well", tuples: []};
			groups.push(groupSrc);
			groups.push(groupDst);
			groups.push(groupWll);
			i--;
		}
	}

	const groups2 = groups.filter(group => group.tuples.length > 0);
	return groups2;
}

function handleGroup(parsed, data, group, groupTypeToFunc) {
	assert(group.tuples.length > 0);

	const tuples = group.tuples;
	// Calculate syringe mask
	const tuple0 = tuples[0];
	const syringeMask = encodeSyringes(tuples);
	// console.log({syringeMask})
	// Volumes for each syringe (in ul)
	const volumes = _.fill(Array(12), "0");
	_.forEach(tuples, tuple => {
		const index = (tuple.syringeRow || 1) - 1;
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
		const program = (func === "Aspirate" && parsed.value.sourceProgram) ? parsed.value.sourceProgram : parsed.value.program;
		const l = [
			syringeMask,
			`"${evowareHelper.stripQuotes(program)}"`,
			volumes.join(","),
			wellInfo.site.evowareGrid, wellInfo.site.evowareSite - 1,
			group.syringeSpacing || 1,
			`"${plateMask}"`,
			0,
			0
		];
		let lines = [{line: `${func}(${l.join(",")});`}];

		// sourceMixing
		if (func === "Aspirate") {
			const mixTuples = (parsed.value.sourceMixing)
				? tuples : tuples.filter(tuple => !_.isUndefined(tuple.item.sourceMixing));
			if (!_.isEmpty(mixTuples)) {
				const count = (mixTuples[0].item.sourceMixing || parsed.value.sourceMixing).count;
				const lines2 = makeLines_Mix(mixTuples, propertyName, parsed.value.sourceMixing, "sourceMixing.volume", parsed.value.program, group.syringeSpacing || 1, count);
				lines = lines2.concat(lines);
			}
		}

		// destinationMixing
		if (func === "Dispense") {
			const mixTuples = (parsed.value.destinationMixing)
				? tuples : tuples.filter(tuple => !_.isUndefined(tuple.item.destinationMixing));
			if (!_.isEmpty(mixTuples)) {
				// console.log(parsed.value.destinationMixing)
				// CONTINUE
				const count = (mixTuples[0].item.destinationMixing || parsed.value.destinationMixing).count;
				const lines2 = makeLines_Mix(mixTuples, propertyName, parsed.value.destinationMixing, "destinationMixing.volume", parsed.value.program, group.syringeSpacing || 1, count);
				lines = lines.concat(lines2);
			}
		}

		return lines;
	}
	function makeLinesMix(propertyName) {
		return makeLines_Mix(tuples, propertyName, undefined, "volume", parsed.value.program, group.syringeSpacing || 1, tuple0.item.count);
	}

	const func = groupTypeToFunc[group.groupType];
	//console.log({syringeMask, volumes})
	return (func === "Mix") ? makeLinesMix(group.groupType) : makeLines(func, group.groupType);
}

//
/**
 * Return Array of size 12 with volumes for each syringe (in ul), taking the
 * values from the 'tuples' items.  For each item, the volume is queried
 * using the volumePropertyName.  If the volume is missing, volumeDefault should
 * be provided.
 *
 * @param  {array} tuples
 * @param  {string} volumePropertyName
 * @param  {mathjs.Unit} volumeDefault
 * @return {array} An array of volumes (in ul)
 */
function makeVolumes(tuples, volumePropertyName, volumeDefault) {
	const volumes = _.fill(Array(12), "0");
	_.forEach(tuples, tuple => {
		const index = (tuple.syringeRow || 1) - 1;
		const ul = _.get(tuple.item, volumePropertyName, volumeDefault).toNumber('ul');
		volumes[index] = `"${math.format(ul, {precision: 14})}"`;
	});
	return volumes;
}

function makeLines_Mix(tuples, propertyName, mixingDefault, volumePropertyName, program, syringeSpacing, count) {
	const syringeMask = encodeSyringes(tuples);
	// console.log({syringeMask})
	const plateMask = encodeWells(tuples, propertyName);
	const volumes = makeVolumes(tuples, volumePropertyName, _.get(mixingDefault, "volume"));
	const wellInfo = tuples[0][propertyName];
	if (_.isUndefined(wellInfo))
		return [];
	const line = new Tokens.Mix({
		syringeMask,
		program,
		volumes,
		evowareGrid: wellInfo.site.evowareGrid,
		evowareSite: wellInfo.site.evowareSite,
		syringeSpacing,
		plateMask,
		count
	}).toLine();
	return [{line}];
}

function handleRetract(parsed, data, groups) {
	const tuplesOrig = _.flatMap(groups, group => group.tuples);
	const tuple0 = _.last(tuplesOrig);
	const propertyName = (tuple0.well) ? "well" : (tuple0.destination) ? "destination" : "source";
	const wellInfo = tuple0[propertyName];
	// console.log({propertyName, wellInfo, tuple0})
	if (_.isUndefined(wellInfo))
		return undefined;

	const labwareModel = wellInfo.labwareModel;

	const syringeRows = _.uniq(tuplesOrig.map(x => x.syringeRow)).sort();
	const tuples = syringeRows.map(syringeRow => ({syringeRow, retract: {row: syringeRow, col: 1, labwareModel}}));
	// Calculate syringe mask
	const syringeMask = encodeSyringes(tuples);

	const retractWellMask = encodeWells(tuples, "retract");
	// console.log({tuples: JSON.stringify(tuples), retractWellMask})
	const lRetract = [
		syringeMask,
		wellInfo.site.evowareGrid, wellInfo.site.evowareSite - 1,
		1, // tip spacing
		`"${retractWellMask}"`,
		4, // 0=positioning with global z travel, 4=z-move
		4, // 4=global z travel
		0,
		400, // speed (mm/s), min 0.1, max 400
		0, 0
	];
	const line = `MoveLiha(${lRetract.join(",")});`;

	return {line};
}

/**
 * Generate a bitmap encoding of syringes to use
 * @param  {array} syringes - array of syringes to use
 * @return {integer} an bitmask encoding of the syringes
 */
function encodeSyringes(tuples) {
	return _.sum(_.map(tuples, tuple => 1 << (tuple.syringeRow - 1)));
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
