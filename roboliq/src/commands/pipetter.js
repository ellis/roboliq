/**
 * Namespace for the ``pipetter`` commands.
 * @namespace pipetter
 * @version v1
 */

/**
 * Pipetter commands module.
 * @module commands/pipetter
 * @return {Protocol}
 * @version v1
 */

const _ = require('lodash');
const assert = require('assert');
const math = require('mathjs');
import yaml from 'yamljs';
const commandHelper = require('../commandHelper.js');
const expect = require('../expect.js');
const misc = require('../misc.js');
const groupingMethods = require('./pipetter/groupingMethods.js');
const pipetterUtils = require('./pipetter/pipetterUtils.js');
const sourceMethods = require('./pipetter/sourceMethods.js');
const wellsParser = require('../parsers/wellsParser.js');
import * as WellContents from '../WellContents.js';

const intensityToValue = {
	"none": 0,
	"flush": 1,
	"light": 2,
	"thorough": 3,
	"decontaminate": 4
};

const valueToIntensity = ["none", "flush", "light", "thorough", "decontaminate"];

function extractLiquidNamesFromContents(contents) {
	if (_.isEmpty(contents) || contents.length < 2) return [];
	if (contents.length === 2 && _.isString(contents[1])) return [contents[1]];
	else {
		return _(contents).tail().map(function(contents2) {
			return extractLiquidNamesFromContents(contents2);
		}).flatten().value();
	}
}

/**
 * Takes a labware name and a well and returns a fully specified well.
 * If the wells is undefined, return undefined.
 * @param  {string} [labwareName] - name of labware for wells that don't have specified labware.
 * @param  {array} [well] - well identifier, with or without labware explicitly specified.
 * @return {array} fully specified well (e.g. on labware).
 */
function getLabwareWell(labwareName, well) {
	if (_.isString(well) && _.isString(labwareName) && !_.isEmpty(labwareName)) {
		return (_.includes(well, "(")) ? well : `${labwareName}(${well})`;
	}

	return well;
}

/**
 * Takes a labware name and a list of wells and returns a list of wells.
 * If the list of wells is empty or undefined, an empty array is returned.
 * @param  {string} [labwareName] - name of labware for wells that don't have specified labware.
 * @param  {array} [wells] - list of wells, with or without labware explicitly specified.
 * @return {array} a list of wells on labware.
 */
function getLabwareWellList(labwareName, wells) {
	const wells1 = wells || [];
	assert(_.isArray(wells1));
	const wells2 = (_.isString(labwareName) && !_.isEmpty(labwareName))
		? _.map(wells1, w => (_.includes(w, "(")) ? w : `${labwareName}(${w})`)
		: wells1;
	return wells2;
}

function pipette(params, parsed, data, options={}) {
	const llpl = require('../HTN/llpl.js').create();
	llpl.initializeDatabase(data.predicates);

	// console.log("pipette: "+JSON.stringify(parsed, null, '\t'))

	// let items = (_.isUndefined(parsed.value.items))
	// 	? []
	// 	: _.flatten(parsed.value.items);
	//console.log("items: "+JSON.stringify(items));
	let agent = parsed.objectName.agent || "?agent";
	let equipmentName = parsed.objectName.equipment || "?equipment";
	//const tipModels = params.tipModels;
	//const syringes = params.syringes;

	// const sourcesTop = getLabwareWellList(parsed.objectName.sourceLabware, parsed.value.sources);
	// //console.log({sourcesTop})
	// const destinationsTop = getLabwareWellList(parsed.objectName.destinationLabware, parsed.value.destinations);
	// const wellsTop = getLabwareWellList(parsed.objectName.wellsLabware, parsed.value.wells);
	// const volumesTop = parsed.value.volumes || [];
	// const syringesTop = (parsed.value.syringes || []).map((x, i) => {
	// 	const syringe = parsed.value.syringes[i];
	// 	if (_.isNumber(syringe))
	// 		return syringe;
	// 	else
	// 		return _.get(parsed.objectName, `syringes.${i}`, syringe);
	// });
	//console.log({sourceLabware})

	const items0 = (parsed.value.items) ? _.flatten(parsed.value.items) : undefined;

	let items = commandHelper.copyItemsWithDefaults(items0, {
		source: parsed.value.sources,
		destination: parsed.value.destinations,
		well: parsed.value.wells,
		volume: parsed.value.volumes,
		syringe: parsed.value.syringes,
		program: parsed.value.program,
		tipModel: parsed.value.tipModel, // TODO: Create a TipModel schema, and then set the tipModel properties in schemas to the "TipModel" type instead of "string"
		distance: parsed.value.distances,
		sourceMixing: parsed.value.sourceMixing,
		destinationMixing: parsed.value.destinationMixing
	});
	// console.log("items: "+JSON.stringify(items))
	if (items.length == 0) {
		return {};
	}

	// 1) Add labware to well properties
	// 2) Fixup mixing specs
	for (let i = 0; i < items.length; i++) {
		const item = items[i];
		if (item.source && parsed.objectName.sourceLabware) {
			item.source = getLabwareWell(parsed.objectName.sourceLabware, item.source);
		}
		if (item.destination && parsed.objectName.destinationLabware) {
			item.destination = getLabwareWell(parsed.objectName.destinationLabware, item.destination);
		}
		if (item.well && parsed.objectName.wellLabware) {
			item.well = getLabwareWell(parsed.objectName.wellLabware, item.well);
		}
		if (item.hasOwnProperty("sourceMixing")) {
			item.sourceMixing = processMixingSpecs([item.sourceMixing]);
		}
		if (item.hasOwnProperty("destinationMixing")) {
			item.destinationMixing = processMixingSpecs([item.destinationMixing]);
		}
	}

	// Calculate volumes from calibrators
	for (let i = 0; i < items.length; i++) {
		const item = items[i];
		if (item.hasOwnProperty("volume")) {
			// Ignore other volume properties
		}
		else if (item.hasOwnProperty("volumeTotal")) {
			// Ignore other volume properties
		}
		else if (item.hasOwnProperty("volumeCalibrated")) {
			const spec = item.volumeCalibrated;
			const calibratorName = spec.calibrator;
			const targetValue = math.eval(spec.value);
			const calibratorVariable = _.get(parsed.orig, ["calibrators", calibratorName, "calibratorVariable"]);
			assert(_.isString(calibratorVariable), "expected calibratorVariable to be a string: "+JSON.stringify(calibratorVariable));
			const calibratorData0 = _.get(parsed.orig, ["calibrators", calibratorName, "calibratorData"]);
			assert(_.isArray(calibratorData0), "expected calibratorData to be an array");
			const calibratorData = _.sortBy(calibratorData0, calibratorVariable);
			const dataLE = _.last(_.filter(calibratorData, x => x[calibratorVariable] <= targetValue));
			const dataGE = _.first(_.filter(calibratorData, x => x[calibratorVariable] >= targetValue));
			const valueLE = math.eval(dataLE[calibratorVariable]);
			const valueGE = math.eval(dataGE[calibratorVariable]);
			const volumeLE = math.eval(dataLE.volume);
			const volumeGE = math.eval(dataGE.volume);
			if (math.equal(valueLE, targetValue)) {
				item.volume = volumeLE;
			}
			else if (math.equal(valueGE, targetValue)) {
				item.volume = volumeGE;
			}
			else {
				const d = math.subtract(valueGE, valueLE);
				const p = math.divide(math.subtract(targetValue, valueLE), d);
				// console.log({d, p})
				// console.log(math.multiply(math.subtract(1, p), volumeLE))
				// console.log(math.multiply(p, volumeGE))
				item.volume = math.add(math.multiply(math.subtract(1, p), volumeLE), math.multiply(p, volumeGE));
			}
			// console.log({spec, dataLE, dataGE, volume: item.volume})
		}
	}

	// In order to handle 'volumeTotal',
	// perform an initial calculation of well volumes, but this will skip source
	// liquids, and therefore need to be performed again later after choosing source wells.
	calculateWellVolumes(items, data);
	for (let i = 0; i < items.length; i++) {
		const item = items[i];
		if (item.hasOwnProperty("volume")) {
			// Ignore other volume properties
		}
		else if (item.hasOwnProperty("volumeTotal")) {
			item.volume = math.subtract(item.volumeTotal, item.volumeBefore);
			// console.log({item})
		}
	}
	// console.log(JSON.stringify(items, null, '  '))

	// Find all wells, both sources and destinations
	const wellName_l = _(items).map(function (item) {
		//console.log({item})
		// TODO: allow source to refer to a set of wells, not just a single well
		// TODO: create a function getSourceWells()
		return [item.source, item.destination, item.well]
	}).flattenDeep().compact().uniq().value();
	// wellName_l = _.uniq(_.compact(_.flattenDeep([wellName_l, sourcesTop, destinationsTop])));
	// console.log("wellName_l", JSON.stringify(wellName_l))

	// Find all labware
	const labwareName_l = _(wellName_l).map(function (wellName) {
		//console.log({wellName})
		const i = wellName.indexOf('(');
		return (i >= 0) ? wellName.substr(0, i) : wellName;
	}).uniq().value();
	const labware_l = _.map(labwareName_l, function (name) { return _.merge({name: name}, expect.objectsValue({}, name, data.objects)); });
	// console.log({labwareName_l, labware_l})

	// Check whether labwares are on sites that can be pipetted
	const query2_l = [];
	_.forEach(labware_l, function(labware) {
		if (!labware.location) {
			return {errors: [labware.name+".location must be set"]};
		}
		const query = {
			"pipetter.canAgentEquipmentSite": {
				"agent": agent,
				"equipment": equipmentName,
				"site": labware.location
			}
		};
		const queryResults = llpl.query(query);
		// console.log("queryResults: "+JSON.stringify(queryResults, null, '\t'));
		if (_.isEmpty(queryResults)) {
			throw {name: "ProcessingError", errors: [labware.name+" is at site "+labware.location+", which hasn't been configured for pipetting; please move it to a pipetting site."]};
		}
		query2_l.push(query);
	});
	// console.log({query2_l})

	// Check whether the same agent and equipment can be used for all the pipetting steps
	if (!_.isEmpty(query2_l)) {
		const query2 = {"and": query2_l};
		//console.log("query2: "+JSON.stringify(query2, null, '\t'));
		const queryResults2 = llpl.query(query2);
		//console.log("query2: "+JSON.stringify(query2, null, '\t'));
		//console.log("queryResults2: "+JSON.stringify(queryResults2, null, '\t'));
		if (_.isEmpty(queryResults2)) {
			return {errors: ["unable to find an agent/equipment combination that can pipette at all required locations: "+_.map(labware_l, function(l) { return l.location; }).join(', ')]}
		}
		// Arbitrarily pick first listed agent/equipment combination
		else {
			const x = queryResults2[0]["and"][0]["pipetter.canAgentEquipmentSite"];
			agent = x.agent;
			equipmentName = x.equipment;
		}
	}

	// Load equipment object
	const equipment = _.get(data.objects, equipmentName);
	assert(equipment, "could not find equipment: "+equipmentName);

	const sourceToItems = _.groupBy(items, 'source');

	// Only keep items that have a positive volume (will need to adapt this for pipetter.punctureSeal)
	if (options.keepVolumelessItems !== true) {
		items = _.filter(items, item => item.volume && item.volume.toNumber('l') > 0);
		// console.log({items})
	}
	if (items.length === 0) {
		return {expansion: []};
	}

	// Any items which have a syringe assigned, if they have a permanent tip model, then set item's tipModel
	for (let i = 0; i < items.length; i++) {
		const item = items[i];
		if (!_.isUndefined(item.syringe)) {
			const syringeName = pipetterUtils.getSyringeName(item.syringe, equipmentName, data);
			const syringe = _.get(data.objects, syringeName);
			if (syringe && syringe.tipModelPermanent)
				item.tipModel = syringe.tipModelPermanent;
		}
	}

	// console.log("A: "+JSON.stringify(_.first(items)))
	// Make sure all items have a 'tipModel' property
	{
		// Try to find tipModel, first for all items
		// Restrict settings to items without tipModel properties
		const items2 = items.filter(x => _.isUndefined(x.tipModel));
		// console.log({items2})
		if (items2.length > 0 && !setTipModel(items2, equipment, equipmentName)) {
			// TODO: Try to find tipModel for each layer
			// Try to find tipModel for each source
			_.forEach(sourceToItems, function(items) {
				const items2 = items.filter(x => _.isUndefined(x.tipModel));
				if (items2.length > 0 && !setTipModel(items2, equipment, equipmentName)) {
					// Try to find tipModel for each item for this source
					_.forEach(items2, function(item) {
						if (!setTipModel([item], equipment, equipmentName)) {
							throw {name: "ProcessingError", message: "no tip model available for item: "+JSON.stringify(item)};
						}
					});
				}
			});
		}
	}
	// console.log("B: "+JSON.stringify(_.first(items)))

	// Make sure all items have a 'program' property
	{
		// Try to find program, first for all items
		const items2 = items.filter(x => _.isUndefined(x.program));
		if (items2.length > 0 && !assignProgram(items2, data)) {
			// Try to find program for each source
			_.forEach(sourceToItems, function(items) {
				const items2 = items.filter(x => _.isUndefined(x.program));
				if (items2.length > 0 && !assignProgram(items, data)) {
					// Try to find program for each item for this source
					_.forEach(items2, function(item) {
						if (!assignProgram([item], data)) {
							throw {name: "ProcessingError", message: "could not automatically choose a program for item: "+JSON.stringify(item)};
						}
					});
				}
			});
		}
	}
	// console.log("C: "+JSON.stringify(_.first(items)))

	// TODO: Limit syringe choices based on params
	const syringesAvailable = _.map(_.keys(equipment.syringe), s => `${equipmentName}.syringe.${s}`) || [];
	const tipModelToSyringes = equipment.tipModelToSyringes;
	// Group the items
	const groups = groupingMethods.groupingMethod3(items, syringesAvailable, tipModelToSyringes);
	// console.log("groups:\n"+JSON.stringify(groups, null, '\t'));

	// Pick syringe for each item
	// For each group assign syringes, starting with the first available one
	_.forEach(groups, function(group) {
		const tipModelToSyringesAvailable = _.cloneDeep(tipModelToSyringes);
		_.forEach(group, function(item) {
			const tipModel = item.tipModel;
			assert(tipModelToSyringesAvailable[tipModel].length >= 1);
			if (_.isUndefined(item.syringe)) {
				item.syringe = tipModelToSyringesAvailable[tipModel].splice(0, 1)[0];
			}
			// TODO: do we need to remove item.syringe from tipModelToSyringesAvailable, it item.syringe was already provided? -- ellis, 2016-03-30
		});
	});

	// Pick source well for items, if the source has multiple wells
	// Rotate through source wells in order of max volume
	for (const group of groups) {
		sourceMethods.sourceMethod3(group, data, effects);
	}

	// Add properties `volumeBefore` and `volumeAfter` to the items.
	calculateWellVolumes(items, data);

	// Calculate when tips need to be washed
	// Create pipetting commands

	const syringeToSource = {};
	// How clean is the syringe/tip currently?
	const syringeToCleanValue = _.fromPairs(_.map(syringesAvailable, s => [s, 5]));
	const expansionList = [];

	/*
	cleanBegin: intensity of first cleaning at beginning of pipetting, before first aspiration.
	Priority: item.cleanBefore || params.cleanBegin || params.clean || source.cleanBefore || "thorough"

	cleanBetween: intensity of cleaning between groups.
	Priority: max(previousCleanAfter, (item.cleanBefore || params.cleanBetween || params.clean || source.cleanBefore || "thorough"))

	previousCleanAfter = item.cleanAfter || if (!params.cleanBetween) source.cleanAfter

	cleanEnd: intensity of cleaning after pipetting is done.
	Priority: max(previousCleanAfter, params.cleanEnd || params.clean || "thorough")
	*/

	// Find the cleaning intensity required before the first aspiration
	const syringeToCleanBeginValue = {};
	_.forEach(groups, function(group) {
		_.forEach(group, function(item) {
			const syringe = item.syringe;
			if (!syringeToCleanBeginValue.hasOwnProperty(syringe)) {
				// TODO: handle source's cleanBefore
				const intensity = item.cleanBefore || parsed.value.cleanBegin || parsed.value.clean || "thorough";
				const intensityValue = intensityToValue[intensity];
				syringeToCleanBeginValue[syringe] = intensityValue;
			}
		});
	});
	// Add cleanBegin commands
	expansionList.push.apply(expansionList, createCleanActions(syringeToCleanBeginValue, agent, equipmentName, data, true));
	//console.log("expansionList:")
	//console.log(JSON.stringify(expansionList, null, '  '));

	// console.log("D: "+JSON.stringify(_.first(groups)))
	const syringeToCleanAfterValue = {};
	let doCleanBefore = false
	_.forEach(groups, function(group) {
		assert(group.length > 0);
		// What cleaning intensity is required for the tip before aspirating?
		const syringeToCleanBeforeValue = _.clone(syringeToCleanAfterValue);
		//console.log({syringeToCleanBeforeValue, syringeToCleanAfterValue})
		_.forEach(group, function(item) {
			const source = item.source || item.well;
			const syringe = item.syringe;
			const isSameSource = (source === syringeToSource[syringe]);

			// Find required clean intensity
			// Priority: max(previousCleanAfter, (item.cleanBefore || params.cleanBetween || params.clean || source.cleanBefore || "thorough"))
			// FIXME: ignore isSameSource if tip has been contaminated by 'Wet' pipetting position
			// FIXME: also take the source's and destination's "cleanBefore" into account
			const intensity = (!isSameSource)
				? item.cleanBefore || parsed.value.cleanBetween || parsed.value.clean || "thorough"
				: item.cleanBefore || parsed.value.cleanBetweenSameSource || parsed.value.cleanBetween || parsed.value.clean || "thorough";
			assert(intensityToValue.hasOwnProperty(intensity));
			let intensityValue = intensityToValue[intensity];
			if (syringeToCleanAfterValue.hasOwnProperty(syringe))
				intensityValue = Math.max(syringeToCleanAfterValue[syringe], intensityValue);
			//console.log({source, syringe, isSameSource, intensityValue})

			// Update cleaning value required before current aspirate
			if (!syringeToCleanBeforeValue.hasOwnProperty(syringe) || intensityValue > syringeToCleanBeforeValue[syringe]) {
				syringeToCleanBeforeValue[syringe] = intensityValue;
			}

			// Set the aspirated source and indicate that the tip is no longer clean
			syringeToSource[syringe] = source;
			syringeToCleanValue[syringe] = 0;
			// FIXME: also consider the source's cleanAfter
			if (item.hasOwnProperty('cleanAfter'))
				syringeToCleanAfterValue[syringe] = item.cleanAfter;
			else
				delete syringeToCleanAfterValue[syringe];
			//console.log({syringeToCleanAfterValue, syringe})
			// TODO: if wet contact, indicate tip contamination
		});

		// Add cleanBefore commands for this group (but not for the first group, because of the cleanBegin section above)
		if (doCleanBefore) {
			expansionList.push.apply(expansionList, createCleanActions(syringeToCleanBeforeValue, agent, equipmentName, data));
		}
		doCleanBefore = true;

		// console.log("E: "+JSON.stringify(_.first(group)))
		// _PipetteItems
		const items2 = _.map(group, function(item) {
			const item2 = _.pick(item, ["syringe", "source", "destination", "well", "volume", "count", "distance"]);
			if (!_.isUndefined(item2.volume)) { item2.volume = item2.volume.format({precision: 14}); }
			if (!_.isUndefined(item2.distance)) { item2.distance = item2.distance.format({precision: 14}); }
			// Mix the source well
			if (item.sourceVolumeBefore && item.sourceMixing) {
				const mixing = item.sourceMixing;
				const volume0 = item.sourceVolumeBefore;
				const volume = calculateMixingVolume(volume0, mixing.amount);
				const mixing2 = {
					count: mixing.count,
					volume: volume.format({precision: 14})
				};
				item2.sourceMixing = mixing2;
			}
			// Mix the destination well
			if (item.volumeAfter && item.destinationMixing) {
				const mixing = item.destinationMixing;
				const volume0 = item.volumeAfter;
				// console.log({mixing, volume0: (volume0) ? volume0 : item})
				const volume = calculateMixingVolume(volume0, mixing.amount);
				const mixing2 = {
					count: mixing.count,
					volume: volume.format({precision: 14})
				};
				item2.destinationMixing = mixing2;
			}
			return item2;
		});
		// console.log("Z: "+JSON.stringify(_.first(items2)))

		// _pipette instruction
		expansionList.push(_.merge({}, {
			"command": "pipetter._pipette",
			"agent": agent,
			"equipment": equipmentName,
			"program": group[0].program,
			"sourceProgram": parsed.value.sourceProgram,
			"items": items2,
		}));
	});

	// cleanEnd
	// Priority: max(previousCleanAfter, params.cleanEnd || params.clean || "thorough")
	const syringeToCleanEndValue = {};
	// console.log({syringeToCleanValue})
	_.forEach(syringeToCleanValue, function (value, syringe) {
		const intensity = parsed.value.cleanEnd || parsed.value.clean || "thorough";
		assert(intensityToValue.hasOwnProperty(intensity));
		let intensityValue = intensityToValue[intensity];
		if (syringeToCleanAfterValue.hasOwnProperty(syringe))
			intensityValue = Math.max(syringeToCleanAfterValue[syringe], intensityValue);
		if (value < intensityValue)
			syringeToCleanEndValue[syringe] = intensityValue;
	});
	//console.log({syringeToCleanEndValue})
	expansionList.push.apply(expansionList, createCleanActions(syringeToCleanEndValue, agent, equipmentName, data));

	// Create the effets object
	// TODO: set final tip clean values
	const effects = {};

	return {
		expansion: expansionList,
		effects: effects
	};
}

// const NOMIXING = {count: 0, amount: 0};
const MIXINGDEFAULT = {count: 3, amount: 0.7};
function processMixingSpecs(l) {
	const mixing = _.reduce(
		l,
		(acc, mixing) => {
			if (_.isUndefined(mixing) || mixing === false) return undefined;
			else if (mixing === true) return {};
			else if (_.isPlainObject(mixing)) return _.merge(acc || {}, mixing);
			return undefined;
		},
		undefined
	);
	_.defaults(mixing, MIXINGDEFAULT);
	return mixing;
}

// Try to find a tipModel for the given items
function findTipModel(items, equipment, equipmentName) {
	/*if (_.size(equipment.tipModel) === 1) {
		const tipModelName = _.keys(equipment.tipModel)[0];
		return `${equipmentName}.tipModel.${tipModelName}`;
	}
	else {*/
		const tipModelName = _.findKey(equipment.tipModel, (tipModel) => {
			return _.every(items, item => {
				const volume = item.volume;
				// Only if the item has a volume, then we'll need a tipModel
				if (!_.isUndefined(volume) && math.compare(volume, math.unit(0, "ul")) > 0) {
					assert(math.unit('l').equalBase(volume), "expected units to be in liters");
					if (math.compare(volume, math.eval(tipModel.min)) < 0 || math.compare(volume, math.eval(tipModel.max)) > 0) {
						return false;
					}
					// TODO: check whether the labware is sealed
					// TODO: check whether the well has cells
				}
				return true;
			});
		});
		return (!_.isEmpty(tipModelName))
			? `${equipmentName}.tipModel.${tipModelName}`
			: undefined;
	// }
}

function setTipModel(items, equipment, equipmentName) {
	assert(!_.isEmpty(items));
	// FIXME: allow for overriding tipModel via top pipetter params
	const tipModelName = findTipModel(items, equipment, equipmentName);
	// console.log({tipModelName, items})
	if (tipModelName) {
		_.forEach(items, function(item) {
			if (!item.tipModel) item.tipModel = tipModelName;
		});
		return true;
	}
	else {
		return false;
	}
}

// Calculate volume for each well or destination,
// adding properties `volumeBefore` and `volumeAfter` to the items.
function calculateWellVolumes(items, data) {
	const wellVolumes = {};
	for (let i = 0; i < items.length; i++) {
		const item = items[i];
		if (_.isString(item.source)) {
			const well = item.source;
			const volume0 = (wellVolumes.hasOwnProperty(well)) ? wellVolumes[well] : WellContents.getWellVolume(well, data)
			const volume1 = math.subtract(volume0, item.volume);
			item.sourceVolumeBefore = volume0;
			item.sourceVolumeAfter = volume1;
			wellVolumes[well] = volume1;
		}
		const well = item.well || item.destination;
		if (well) {
			const volume0 = (wellVolumes.hasOwnProperty(well)) ? wellVolumes[well] : WellContents.getWellVolume(well, data)
			const volume1 = (item.destination && item.volume)
				? math.add(volume0, item.volume)
				: volume0;
			item.volumeBefore = volume0;
			item.volumeAfter = volume1;
			wellVolumes[well] = volume1;
			// console.log({well, volume: wellVolumes[well]})
		}
	}
}

// Try to find a pipettingClass for the given items
function findPipettingClass(items, data) {
	// Pick liquid properties by inspecting source contents
	const pipettingClasses0 = items.map(item => {
		let pipettingClass = "Water";
		const source0 = item.source || item.well || item.destination; // If no source is provided, then use well or destination
		const source = commandHelper.asArray(source0);

		// FIXME: for debug only
		if (!source || _.isEmpty(source)) {
			console.log({item});
		}
		// ENDFIX

		//console.log({source})
		if (source.length > 0) {
			//console.log({source})
			const contents = WellContents.getWellContents(source[0], data);
			if (contents) {
				const liquids = extractLiquidNamesFromContents(contents);
				const pipettingClasses = _(liquids).map(function(name) {
					return misc.findObjectsValue(name+".pipettingClass", data.objects, null, "Water");
				}).uniq().value();
				// FIXME: should pick "Water" if water-like liquids have high enough concentration
				// Use "Water" if present
				if (!_.includes(pipettingClasses, "Water")) {
					if (pipettingClasses.length === 1) {
						pipettingClass = pipettingClasses[0];
					}
					else if (pipettingClasses.length > 1) {
						pipettingClass = null;
					}
				}
			}
		}

		return pipettingClass;
	});
	const pipettingClasses = _.uniq(pipettingClasses0);

	if (pipettingClasses.length === 1) {
		return pipettingClasses[0];
	}
	else {
		return null;
	}
}

// Pick position (wet or dry) by whether there are already contents in the destination well
function findPipettingPosition(items, data) {
	const pipettingPositions = _(items).map(item => item.destination || item.well).compact().map(function(well) {
		const i = well.indexOf('(');
		const labware = well.substr(0, i);
		const wellId = well.substr(i + 1, 3); // FIXME: parse this instead, allow for A1 as well as A01
		const contents = misc.findObjectsValue(labware+".contents."+wellId, data.objects);
		const liquids = extractLiquidNamesFromContents(contents);
		return _.isEmpty(liquids) ? "Dry" : "Wet";
	}).uniq().value();

	if (pipettingPositions.length === 1) {
		return pipettingPositions[0];
	}
	else {
		return null;
	}
}

function assignProgram(items0, data) {
	// console.log("assignProgram: "+JSON.stringify(items))
	// items0.forEach(x => console.log(JSON.stringify(x)))
	// console.log({items0})
	const items = items0.filter(item => item.volume && math.larger(item.volume, math.unit(0, "l")));
	if (items.length > 0) {
		// console.log({items})
		const pipettingClass = findPipettingClass(items, data);
		if (!pipettingClass) return false;
		const pipettingPosition = findPipettingPosition(items, data);
		if (!pipettingPosition) return false;
		const tipModels = _(items).map('tipModel').compact().uniq().value();
		if (tipModels.length !== 1) return false;
		const tipModelName = tipModels[0];
		assert(tipModelName, `missing value for tipModelName: `+JSON.stringify(tipModels));
		const tipModelCode = misc.getObjectsValue(tipModelName+".programCode", data.objects);
		//console.log({equipment})
		assert(tipModelCode, `missing value for ${tipModelName}.programCode`);
		const program = "\"Roboliq_"+pipettingClass+"_"+pipettingPosition+"_"+tipModelCode+"\"";
		_.forEach(items0, function(item) { item.program = program; });
	}
	return true;
}

// Create clean commands before pipetting this group
function createCleanActions(syringeToCleanValue, agent, equipmentName, data, compareToOriginalState = false) {
	// console.log("createCleanActions: "+JSON.stringify(syringeToCleanValue))
	const items = _(syringeToCleanValue).toPairs().map(([syringeName0, n]) => {
		if (n > 0) {
			const syringeName = pipetterUtils.getSyringeName(syringeName0, equipmentName, data);
			const syringe = commandHelper._g(data, syringeName);
			if (compareToOriginalState) {
				const intensity = syringe.cleaned;
				const syringeCleanedValue = intensityToValue[syringe.cleaned] || 0;
				// console.log({syringeName0, n, syringeName, intensity, syringeCleanedValue, syringe})
				if (n > syringeCleanedValue)
					return {syringe: syringeName, intensity: valueToIntensity[n]};
			}
			else {
				return {syringe: syringeName, intensity: valueToIntensity[n]};
			}
		}
	}).compact().value();
	// console.log({cleanItems: items})
	if (_.isEmpty(items)) return [];
	return [{
		command: "pipetter.cleanTips",
		agent: agent,
		equipment: equipmentName,
		items
	}];
}

/*
// Mix destination after dispensing?
function addMixing(parsed, agent, equipmentName, group, mixPropertyName, wellPropertyName, volumePropertyName) {
	let mixItems = [];
	_.forEach(group, function(item) {
		const well = item[wellPropertyName];
		const doMixing = !_.isUndefined(well) && _.get(item, mixPropertyName, _.get(parsed.value, mixPropertyName, false));
		if (doMixing) {
			const mixing = _.defaults({count: 3, amount: 0.7}, item[mixPropertyName], parsed.value[mixPropertyName]);
			const volume0 = item[volumePropertyName];
			const volume = calculateMixingVolume(volume0, mixing.amount);
			const mixItem = _.merge({}, {
				syringe: item.syringe,
				well,
				count: mixing.count,
				volume: volume.format({precision: 14})
			});
			mixItems.push(mixItem);
		}
	});
	if (mixItems.length > 0) {
		const mixCommand = {
			command: "pipetter._mix",
			agent,
			equipment: equipmentName,
			program: group[0].program, // FIXME: even if we used Air dispense for the dispense, we need to use Wet or Bot here
			items: mixItems
		};
		return mixCommand;
	}
	return undefined;
}
*/

function calculateMixingVolume(volume0, amount) {
	amount = _.isString(amount) ? math.eval(amount) : amount;
	// console.log("amount: "+JSON.stringify(amount))
	// console.log("type: "+math.typeof(amount))
	switch (math.typeof(amount)) {
		case "number":
		case "BigNumber":
		case "Fraction":
			// assert(amount >= 0 && amount < 1, "amount must be between 0 and 1: "+JSON.stringify(item));
			return math.multiply(volume0, amount);
		case "Unit":
			return amount;
	}
	assert(false, "expected amount to be a volume or a number: "+JSON.amount);
}

/**
 * Handlers for {@link pipetter} commands.
 * @static
 */
const commandHandlers = {
	"pipetter._aspirate": function(params, parsed, data) {
		// console.log("params", JSON.stringify(params, null, '  '))
		const effects = pipetterUtils.getEffects_pipette(parsed, data);
		// console.log("effects:", JSON.stringify(effects, null, '  '))
		return {effects};
	},
	"pipetter._dispense": function(params, parsed, data) {
		//console.log("params", JSON.stringify(params, null, '  '))
		const effects = pipetterUtils.getEffects_pipette(parsed, data);
		//console.log("effects:", JSON.stringify(effects, null, '  '))
		return {effects};
	},
	"pipetter._measureVolume": function(params, parsed, data) {
		// console.log("pipetter._punctureSeal: "+JSON.stringify(parsed, null, '\t'))
		const effects = pipetterUtils.getEffects_pipette(parsed, data);
		return {
			effects,
			reports: (_.isEmpty(data.objects.DATA)) ? undefined : {
				measurementFactors: data.objects.DATA
			}
		};
	},
	"pipetter._mix": function(params, parsed, data) {
		// console.log("pipetter._mix: "+JSON.stringify(parsed, null, '\t'))
		parsed.value.items = commandHelper.copyItemsWithDefaults(parsed.value.items, parsed.value.itemDefaults);
		//console.log("params", JSON.stringify(params, null, '  '))
		//console.log("effects:", JSON.stringify(pipetterUtils.getEffects_pipette(params, data), null, '  '))
		return {
			effects: pipetterUtils.getEffects_pipette(parsed, data)
		};
	},
	"pipetter._pipette": function(params, parsed, data) {
		// console.log("params", JSON.stringify(params, null, '  '))
		const effects = pipetterUtils.getEffects_pipette(parsed, data);
		// console.log("effects:", JSON.stringify(effects, null, '  '))
		return {effects};
	},
	"pipetter._punctureSeal": function(params, parsed, data) {
		// console.log("pipetter._punctureSeal: "+JSON.stringify(parsed, null, '\t'))
		const effects = pipetterUtils.getEffects_pipette(parsed, data);
		// Add effects for seal punctures
		_.forEach(parsed.value.items, item => {
			const wellInfo = wellsParser.parseOne(item.well);
			const labwareName = wellInfo.source || wellInfo.labware;
			const id = `${labwareName}.sealPunctures.${wellInfo.wellId}`;
			if (_.get(data.objects, id) !== true) {
				effects[id] = true;
			}
		});
		return { effects };
	},
	"pipetter._washTips": function(params, parsed, data) {
		//console.log("_washTips:");
		//console.log(JSON.stringify(parsed, null, '\t'))
		const effects = {};
		parsed.value.syringes.forEach((syringe, index) => {
			const syringeName = parsed.objectName[`syringes.${index}`];
			if (!_.isUndefined(syringe.contaminants))
				effects[`${syringeName}.contaminants`] = null;
			// Remove contents property
			if (!_.isUndefined(syringe.contents))
				effects[`${syringeName}.contents`] = null;
			// Set cleaned property
			if (syringe.cleaned !== parsed.value.intensity)
				effects[`${syringeName}.cleaned`] = parsed.value.intensity;
		});
		return {effects};
	},
	"pipetter.cleanTips": function(params, parsed, data) {
		// console.log("pipetter.cleanTips:")
		// console.log(JSON.stringify(parsed, null, '\t'));

		const syringes0 = (params.syringes)
			? commandHelper.asArray(params.syringes)
			: (!params.items && parsed.value.equipment && parsed.value.equipment.syringe)
				? _.keys(parsed.value.equipment.syringe).map(s => parsed.objectName.equipment + ".syringe." + s)
				: [];
		const n = _.max([syringes0.length, commandHelper.asArray(params.items).length])
		const itemsToMerge = [
			syringes0.map(syringe => { return {syringe} }),
			(params.intensity) ? _.times(n, () => ({intensity: params.intensity})) : []
		];
		const items = _.merge([], itemsToMerge[0], itemsToMerge[1], params.items);
		//console.log("items: "+JSON.stringify(params.items))
		//console.log(JSON.stringify(itemsToMerge, null, '\t'));
		//console.log("items: "+JSON.stringify(items))

		// Ensure fully qualified names for the syringes
		_.forEach(items, item => {
			if (_.isInteger(item.syringe)) {
				item.syringe = `${parsed.objectName.equipment}.syringe.${item.syringe}`;
			}
		});

		// Get list of valid agent/equipment/syringe combinations for all syringes
		const nodes = _.flatten(items.map(item => {
			const predicates = [
				{"pipetter.canAgentEquipmentSyringe": {
					"agent": parsed.objectName.agent,
					"equipment": parsed.objectName.equipment,
					syringe: item.syringe
				}}
			];
			//console.log(predicates)
			const alternatives = commandHelper.queryLogic(data, predicates, '[].and[]."pipetter.canAgentEquipmentSyringe"');
			expect.truthy({paramName: "items"}, !_.isEmpty(alternatives), `could not find agent and equipment to clean syring ${item.syringe}`);
			return alternatives;
		}));
		//console.log(nodes);
		// Group by agent+equipment
		const equipToNodes = _.groupBy(nodes, x => `${x.agent}|${x.equipment}`);
		//console.log(equipToNodes);
		// Group by syringe
		const syringeToNodes = _.groupBy(nodes, x => x.syringe);
		// console.log({syringeToNodes});

		// Desired intensity for each syringe
		const syringeToItem = _.groupBy(items, item => item.syringe);
		// Sub-command list
		let expansion = [];
		// Get list of syringes
		let syringesRemaining = _.uniq(items.map(item => item.syringe));
		//console.log({nodes, syringeToNodes, syringeToItem})
		// Generate sub-commands until all syringes have been taken care of
		while (!_.isEmpty(syringesRemaining)) {
			const syringe = syringesRemaining[0];
			const nodes = syringeToNodes[syringe];
			// console.log({syringe, nodes})
			// Arbitrarily pick the first possible agent/equipment combination
			const {agent, equipment} = nodes[0];
			const equipNodes = equipToNodes[`${agent}|${equipment}`];
			const syringes = _.intersection(syringesRemaining, equipNodes.map(x => x.syringe));
			// Create cleanTips items
			const items = _.flatten(syringes.map(syringe => syringeToItem[syringe]));
			//console.log({syringes, syringeToItem, items})
			// Add the sub-command
			expansion.push({
				command: `pipetter.cleanTips|${agent}|${equipment}`,
				agent,
				equipment,
				items
			});
			// Remove those syringes from the remaining list
			syringesRemaining = _.difference(syringesRemaining, syringes);
		}
		//console.log(expansion);

		return {expansion};
	},
	"pipetter.measureVolume": function(params, parsed, data) {
		// console.log("pipetter.measureVolume: "+JSON.stringify(parsed))

		const items = commandHelper.copyItemsWithDefaults(parsed.value.items, {
			well: parsed.value.wells,
		});
		// console.log("items: "+JSON.stringify(items))

		// Add labware to well properties
		for (let i = 0; i < items.length; i++) {
			const item = items[i];
			if (item.well && parsed.objectName.wellLabware) {
				item.well = getLabwareWell(parsed.objectName.wellLabware, item.well);
			}
		}

		const parsed2 = _.cloneDeep(parsed);
		// _.merge(parsed2.value, defaults3);
		parsed2.value.items = items;

		const result = pipette(params, parsed2, data, {keepVolumelessItems: true});

		_.forEach(result.expansion, step => {
			if (step.command === "pipetter._pipette") {
				step.command = "pipetter._measureVolume";
			}
		});

		return result;
	},
	"pipetter.mix": function(params, parsed, data) {
		// console.log("pipetter.mix: "+JSON.stringify(parsed))

		const items = commandHelper.copyItemsWithDefaults(parsed.value.items, {
			well: parsed.value.wells,
			count: parsed.value.counts,
			amount: parsed.value.amounts
		});
		// console.log("items: "+JSON.stringify(items))

		// Add labware to well properties
		for (let i = 0; i < items.length; i++) {
			const item = items[i];
			if (item.well && parsed.objectName.wellLabware) {
				item.well = getLabwareWell(parsed.objectName.wellLabware, item.well);
			}
		}

		const items2 = _.map(items, (item, i) => {
			assert(item.well, `missing well for mix item ${i}: ${JSON.stringify(item)}`);
			const volume0 = WellContents.getWellVolume(item.well, data);
			assert(math.compare(volume0, math.unit(0, 'l')) > 0, "cannot mix empty wells");

			const item2 = _.omit(item, ["amount"]);
			item2.volume = calculateMixingVolume(volume0, item.amount);

			return item2;
		});

		const parsed2 = _.cloneDeep(parsed);
		// _.merge(parsed2.value, defaults3);
		parsed2.value.items = items2;

		const result = pipette(params, parsed2, data);

		_.forEach(result.expansion, step => {
			if (step.command === "pipetter._pipette") {
				step.command = "pipetter._mix";
				const {items: items3, defaults: defaults3} = commandHelper.splitItemsAndDefaults(step.items, ["syringe", "well"]);
				// console.log({items3, defaults3});
				if (!_.isEmpty(defaults3))
					step.itemDefaults = defaults3;
				step.items = items3;
			}
		});

		return result;
	},
	"pipetter.pipette": pipette,
	"pipetter.pipetteDilutionSeries": function(params, parsed, data) {
		// console.log("pipetter.pipetteDilutionSeries: "+JSON.stringify(parsed, null, '\t'))
		const destinationLabware = parsed.objectName.destinationLabware;
		const dilutionMethod = parsed.value.dilutionMethod;

		// Fill all destination wells with diluent
		const diluentItems = [];
		const items = [];
		_.forEach(parsed.value.items, (item, itemIndex) => {
			if (_.isEmpty(item.destinations)) return;
			// FIXME: handle `source`
			assert(_.isUndefined(item.source), "`source` property not implemented yet");
			const destinations1 = item.destinations.map(s => getLabwareWell(destinationLabware, s));
			const destination0 = destinations1[0];
			const destinations2 = _.tail(destinations1);
			const syringeName = parsed.objectName[`items.${itemIndex}.syringe`] || item.syringe;
			// console.log({destination0, destinations2, syringeName})
			let dilutionFactorPrev = 1;

			// get volume of destination0
			const volume0 = WellContents.getWellVolume(destination0, data);
			assert(math.compare(volume0, math.unit(0, 'l')) > 0, "first well in dilution series shouldn't be empty");

			// The target volume of the dilution wells (or take the volume of the first 'destination')
			const volumeFinal = parsed.value.volume || volume0;

			// Dilute the destination0, if necessary
			if (math.smaller(volume0, volumeFinal)) {
				assert(parsed.objectName.diluent, "missing 'diluent'");
				const diluentVolume2 = math.subtract(volumeFinal, volume0);
				const item2 = {
					source: parsed.objectName.diluent,
					destination: getLabwareWell(destinationLabware, destination0),
					volume: diluentVolume2.format({precision: 4}),
					syringe: syringeName,
				};
				diluentItems.push(item2);
			}

			// Calculate volume to transfer from one well to the next, and the diluent volume
			const sourceVolume = (dilutionMethod === "source")
				? volumeFinal : math.divide(volumeFinal, parsed.value.dilutionFactor);
			const diluentVolume = (dilutionMethod === "source")
			 	? volumeFinal : math.subtract(volumeFinal, sourceVolume);
			// console.log({volume0: volume0.format(), sourceVolume: sourceVolume.format(), diluentVolume: diluentVolume.format()})

			// If we want to pre-dispense the diluent:
			if (dilutionMethod === "begin") {
				// Distribute diluent to all destination wells
				// If 'lastWellHandling == none', don't dilute the last well
				const destinations3 = (parsed.value.lastWellHandling !== "none") ? destinations2 : _.initial(destinations2);
				_.forEach(destinations3, (destinationWell, index) => {
					const wellContents = WellContents.getWellContents(destinationWell, data);
					const wellVolume = WellContents.getVolume(wellContents);
					if (math.smaller(wellVolume, diluentVolume)) {
						assert(parsed.objectName.diluent, "missing 'diluent'");
						const diluentVolume2 = math.subtract(diluentVolume, wellVolume);
						const item2 = {
							layer: index+1,
							source: parsed.objectName.diluent,
							destination: getLabwareWell(destinationLabware, destinationWell),
							volume: diluentVolume2.format({precision: 4}),
							syringe: syringeName,
						};
						diluentItems.push(item2);
					}
				});
			}
			// console.log({diluentItems})

			// Pipette the dilutions
			let source = destination0;
			_.forEach(destinations2, (destinationWell, index) => {
				const destination = getLabwareWell(destinationLabware, destinationWell);
				// Dilute the source first?
				if (dilutionMethod === "source") {
					const layer = (index + 1) * 2 - 1;
					const volume = math.subtract(math.multiply(volumeFinal, parsed.value.dilutionFactor), volumeFinal);
					const item2 = {
						layer,
						source: parsed.objectName.diluent,
						destination: source,
						volume: volume.format({precision: 4}),
						syringe: syringeName,
						sourceMixing: false,
						destinationMixing: false
					};
					items.push(item2);
				}
				// Transfer to destination
				{
					const layer = (dilutionMethod !== "begin") ? (index + 1) * 2 : index + 1;
					// console.log({dilutionMethod, index, layer})
					const item2 = {
						layer, source, destination,
						volume: sourceVolume.format({precision: 4}),
						syringe: syringeName
					};
					// Mix before aspirating from first dilution well
					if (index === 0 || dilutionMethod === "source") {
						item2.sourceMixing = _.get(parsed.value, "sourceMixing", true);
					}
					items.push(item2);
				}
				source = destination;
			});

			// May need to extract aliquot from the final destination well in order to
			// get it to the proper volume
			if (dilutionMethod !== "source") {
				// If disposal wells are specified, transfer extra volume from last well to the disposal
				// FIXME: implement sending last aspirate to TRASH!
				// Create final aspiration
				items.push({
					layer: (dilutionMethod === "begin") ? destinations2.length + 1 : (destinations2.length + 1) * 2 - 1,
					source: getLabwareWell(destinationLabware, _.last(destinations2)),
					volume: sourceVolume.format({precision: 4}),
					syringe: syringeName
				});
			}

			/*const source = (firstItemIsSource) ? dilution0.destination : dilution0.source;
			_.forEach(series, dilution => {
				// If the first item doesn't define a source, but it's dilutionFactor = 1, then treat the destination well as the source.
				assert(!_.isUndefined(source), "dilution item requires a source");
				diluentItems.push({source: parsed.objectName.diluent, destination})
			});
			*/
		});

		//const items = [];

		const expansion = [];

		if (diluentItems.length > 0) {
			// Cleaning:
			// if 'items' is empty,
			const params1 = _.pick(parsed.orig, ["destinationLabware", "sourceLabware", "syringes"]);
			params1.command = "pipetter.pipette";
			params1.items = diluentItems;
			if (parsed.value.cleanBegin) params1.cleanBegin = parsed.value.cleanBegin;
			params1.cleanBetweenSameSource = "none";
			if (items.length > 0) params1.cleanEnd = "none";
			else if (parsed.value.cleanEnd) params1.cleanEnd = parsed.value.cleanEnd;
			_.merge(params1, parsed.orig.diluentParams);
			expansion.push(params1);
		}

		if (items.length > 0) {
			const params2 = _.pick(parsed.orig, ["destinationLabware", "sourceLabware", "syringes"]);
			params2.command = "pipetter.pipette";
			params2.items = items;
			if (diluentItems.length > 0) params2.cleanBegin = "none";
			else if (parsed.value.cleanBegin) params2.cleanBegin = parsed.value.cleanBegin;
			params2.cleanBetweenSameSource = "none";
			if (parsed.value.cleanEnd) params2.cleanEnd = parsed.value.cleanEnd;
			const destinationMixing = (dilutionMethod === "begin") ? true : false;
			_.defaults(params2, parsed.value.dilutionParams, {cleanBetween: "none", destinationMixing});
			expansion.push(params2);
			// console.log({params1, params2})
		}

		return { expansion };
	},
	"pipetter.pipetteMixtures": function(params, parsed, data) {
		// console.log("pipetter.pipetteMixtures: "+JSON.stringify(parsed, null, '\t'));
		// Obtain a matrix of mixtures (rows for destinations, columns for layers)
		const mixtures0 = parsed.value.mixtures.map((item, index1) => {
			//console.log({index1, destination: item.destination || _.get(parsed.value.destinations, index1), destinations: _.get(parsed.value.destinations, index1)})
			const {destination, syringe, sources} = (_.isPlainObject(item))
				? {destination: item.destination || _.get(parsed.value.destinations, index1), syringe: item.syringe, sources: item.sources}
				: {destination: _.get(parsed.value.destinations, index1), syringe: undefined, sources: item};
			return sources.map((subitem, index2) => {
				// If the layer is empty, ignore it
				if (!_.isEmpty(subitem)) {
					// Fill in destination and syringe defaults for current destination+layer
					const item2 = _.merge({},
						{destination, syringe},
						subitem,
						{index: index2}
					);
					//console.log({item2})
					return item2;
				}
				else {
					return [];
				}
			});
		});
		//const destinations = parsed.value.destinations;
		//console.log("params:", params);
		//console.log("data.objects.mixtures:", data.objects.mixtures);
		//console.log("mixtures:\n"+JSON.stringify(mixtures0));
		//console.log("A:", misc.getVariableValue(params.destinations, data.objects))
		//console.log("data.objects.mixtureWells:", data.objects.mixtureWells);
		//console.log("destinations:", destinations);
		//expect.truthy({}, destinations.length >= params.mixtures.length, "length of destinations array must be equal or greater than length of mixtures array.");

		const mixtures = _.compact(mixtures0);
		//console.log("mixtures:", mixtures);

		const params2 = _.omit(params, ['mixtures', 'destinations', 'order']);

		let order = parsed.value.order || ["index"];
		if (!_.isArray(order)) order = [order];
		//console.log("A:", params2.items)
		params2.items = _(mixtures).flatten().compact().sortBy(order).map(item => _.omit(item, 'index')).value();
		// console.log("B:", params2.items)
		params2.command = "pipetter.pipette";
		return {
			expansion: {
				"1": params2
			}
		};
	},
	"pipetter.punctureSeal": function(params, parsed, data) {
		const result = pipette(params, parsed, data, {keepVolumelessItems: true});

		_.forEach(result.expansion, step => {
			if (step.command === "pipetter._pipette") {
				step.command = "pipetter._punctureSeal";
			}
			delete step.program;
		});

		return result;
	},
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+'/../schemas/pipetter.yaml'),
	commandHandlers: commandHandlers
};
