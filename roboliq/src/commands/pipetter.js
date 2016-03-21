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

var _ = require('lodash');
var assert = require('assert');
var math = require('mathjs');
import yaml from 'yamljs';
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');
var groupingMethods = require('./pipetter/groupingMethods.js');
var pipetterUtils = require('./pipetter/pipetterUtils.js');
var sourceMethods = require('./pipetter/sourceMethods.js');
var wellsParser = require('../parsers/wellsParser.js');
import * as WellContents from '../WellContents.js';

var intensityToValue = {
	"none": 0,
	"flush": 1,
	"light": 2,
	"thorough": 3,
	"decontaminate": 4
};

var valueToIntensity = ["none", "flush", "light", "thorough", "decontaminate"];

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

function pipette(params, parsed, data) {
	var llpl = require('../HTN/llpl.js').create();
	llpl.initializeDatabase(data.predicates);

	//console.log("pipette: "+JSON.stringify(parsed, null, '\t'))

	let items = (_.isUndefined(parsed.value.items))
		? []
		: _.flatten(parsed.value.items);
	//console.log("items: "+JSON.stringify(items));
	let agent = parsed.objectName.agent || "?agent";
	let equipmentName = parsed.objectName.equipment || "?equipment";
	//var tipModels = params.tipModels;
	//var syringes = params.syringes;

	const sourcesTop = getLabwareWellList(parsed.objectName.sourceLabware, parsed.value.sources);
	//console.log({sourcesTop})
	const destinationsTop = getLabwareWellList(parsed.objectName.destinationLabware, parsed.value.destinations);
	const volumesTop = parsed.value.volumes || [];
	const syringesTop = (parsed.value.syringes || []).map((x, i) => {
		const syringe = parsed.value.syringes[i];
		if (_.isNumber(syringe))
			return syringe;
		else
			return _.get(parsed.objectName, `syringes.${i}`, syringe);
	})
	//console.log({sourceLabware})

	// Figure out number of items
	var itemCount = 0;
	if (items.length > 0) itemCount = items.length;
	else itemCount = Math.max(sourcesTop.length, destinationsTop.length, volumesTop.length);

	// Populate item properties from the top properties
	if (itemCount > 0) {
		// Check for compatible lengths
		if (items.length > 0) {
			if (sourcesTop.length > 1) assert(sourcesTop.length == itemCount, "`sources` length and `items` length must be equal")
			if (destinationsTop.length > 1) assert(destinationsTop.length == itemCount, "`destinations` length and `items` length must be equal")
			if (volumesTop.length > 1) assert(volumesTop.length == itemCount, "`volumes` length and `items` length must be equal")
		}
		else {
			if (sourcesTop.length > 1 && destinationsTop.length > 1) assert(sourcesTop.length == destinationsTop.length, "`sources` length and `destinations` length must be equal")
			if (sourcesTop.length > 1 && volumesTop.length > 1) assert(sourcesTop.length == volumesTop.length, "`sources` length and `volumes` length must be equal")
			if (destinationsTop.length > 1 && volumesTop.length > 1) assert(destinationsTop.length == volumesTop.length, "`destinations` length and `volumes` length must be equal")
		}

		if (items.length == 0) {
			items = Array(itemCount);
			for (var i = 0; i < itemCount; i++)
				items[i] = {};
		}

		for (var i = 0; i < itemCount; i++) {
			const item = items[i];

			// Add index to all items, so that we can reference `parsed.objectName[items.$index.syringe]` later
			item.index = i;

			//console.log("i:", i)
			if (_.isUndefined(item.source)) {
				if (sourcesTop.length == 1)
					item.source = sourcesTop[0];
				else if (sourcesTop.length > 1)
					item.source = sourcesTop[i];
			}
			else if (parsed.objectName.sourceLabware) {
				item.source = getLabwareWell(parsed.objectName.sourceLabware, item.source);
			}

			if (_.isUndefined(item.destination)) {
				//console.log("step", item, destinationsTop, i, destinationsTop[i])
				if (destinationsTop.length == 1)
					item.destination = destinationsTop[0];
				else if (destinationsTop.length > 1)
					item.destination = destinationsTop[i];
			}
			if (parsed.objectName.destinationLabware) {
				item.destination = getLabwareWell(parsed.objectName.destinationLabware, item.destination);
				// console.log({item_destinations: items.map(x => x.destination)})
			}

			if (_.isUndefined(item.volume)) {
				//console.log(`items[${i}].volume is undefined, volumesTop = ${volumesTop}`)
				if (volumesTop.length == 1)
					item.volume = volumesTop[0];
				else if (volumesTop.length > 1)
					item.volume = volumesTop[i];
			}

			if (_.isUndefined(item.syringe)) {
				if (syringesTop.length == 1)
					item.syringe = syringesTop[0];
				else if (syringesTop.length > 1) {
					item.syringe = syringesTop[i];
				}
			}
			// Otherwise replace syringe objects with syringe names
			else {
				item.syringe = _.get(parsed.objectName, `items.${i}.syringe`, item.syringe);
			}
		}
	}
	//console.log(JSON.stringify(sourcesTop, null, '  '))
	// console.log(JSON.stringify(items, null, '  '))

	// Find all wells, both sources and destinations
	var wellName_l = _(items).map(function (item) {
		//console.log({item})
		// TODO: allow source to refer to a set of wells, not just a single well
		// TODO: create a function getSourceWells()
		return [item.source, item.destination]
	}).flatten().compact().value();
	wellName_l = _.uniq(_.compact(_.flattenDeep([wellName_l, sourcesTop, destinationsTop])));
	// console.log("wellName_l", JSON.stringify(wellName_l))

	// Find all labware
	var labwareName_l = _(wellName_l).map(function (wellName) {
		//console.log({wellName})
		var i = wellName.indexOf('(');
		return (i >= 0) ? wellName.substr(0, i) : wellName;
	}).uniq().value();
	var labware_l = _.map(labwareName_l, function (name) { return _.merge({name: name}, expect.objectsValue({}, name, data.objects)); });

	// Check whether labwares are on sites that can be pipetted
	var query2_l = [];
	_.forEach(labware_l, function(labware) {
		if (!labware.location) {
			return {errors: [labware.name+".location must be set"]};
		}
		var query = {
			"pipetter.canAgentEquipmentSite": {
				"agent": agent,
				"equipment": equipmentName,
				"site": labware.location
			}
		};
		var queryResults = llpl.query(query);
		//console.log("queryResults: "+JSON.stringify(queryResults, null, '\t'));
		if (_.isEmpty(queryResults)) {
			throw {name: "ProcessingError", errors: [labware.name+" is at site "+labware.location+", which hasn't been configured for pipetting; please move it to a pipetting site."]};
		}
		query2_l.push(query);
	});
	// console.log({query2_l})

	// Check whether the same agent and equipment can be used for all the pipetting steps
	if (!_.isEmpty(query2_l)) {
		var query2 = {"and": query2_l};
		//console.log("query2: "+JSON.stringify(query2, null, '\t'));
		var queryResults2 = llpl.query(query2);
		//console.log("query2: "+JSON.stringify(query2, null, '\t'));
		//console.log("queryResults2: "+JSON.stringify(queryResults2, null, '\t'));
		if (_.isEmpty(queryResults2)) {
			return {errors: ["unable to find an agent/equipment combination that can pipette at all required locations: "+_.map(labware_l, function(l) { return l.location; }).join(', ')]}
		}
		// Arbitrarily pick first listed agent/equipment combination
		else {
			var x = queryResults2[0]["and"][0]["pipetter.canAgentEquipmentSite"];
			agent = x.agent;
			equipmentName = x.equipment;
		}
	}

	const parsed2 = commandHelper.parseParams({equipment: equipmentName}, data, {
		properties: {
			equipment: {type: "Equipment"}
		},
		required: ["equipment"]
	});
	//console.log({equipment: parsed2.equipment.value})
	const equipment = parsed2.value.equipment;

	// TODO: if labwares are not on sites that can be pipetted, try to move them to appropriate sites

	// Try to find a tipModel for the given items
	function findTipModel(items) {
		const tipModelName = _.findKey(equipment.tipModel, (tipModel) => {
			return _.every(items, item => {
				const volume = item.volume;
				assert(math.unit('l').equalBase(volume), "expected units to be in liters");
				if (math.compare(volume, math.eval(tipModel.min)) < 0 || math.compare(volume, math.eval(tipModel.max)) > 0) {
					return false;
				}
				// TODO: check whether the labware is sealed
				// TODO: check whether the well has cells
				return true;
			});
		});
		return (!_.isEmpty(tipModelName))
			? `${equipmentName}.tipModel.${tipModelName}`
			: undefined;
	}
	function setTipModel(items) {
		const parsed3 = { orig: { items } };
		const tipModelName = findTipModel(null, parsed3, data);
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

	// FIXME: allow for overriding tipModel via params

	var sourceToItems = _.groupBy(items, 'source');

	const itemsAll = items;
	//console.log({itemVolumes: items.map(x => x.volume)})
	//console.log(_.filter(items, item => item.volume));
	items = _.filter(items, item => item.volume && item.volume.toNumber('l') > 0);

	// Try to find tipModel, first for all items
	if (!setTipModel(items)) {
		// TODO: Try to find tipModel for each layer
		// Try to find tipModel for each source
		_.forEach(sourceToItems, function(items) {
			if (!setTipModel(items)) {
				// Try to find tipModel for each item for this source
				_.forEach(items, function(item) {
					if (!setTipModel([item])) {
						throw {name: "ProcessingError", message: "no tip model available for item: "+JSON.stringify(item)};
					}
				});
			}
		});
	}

	// Try to find a pipettingClass for the given items
	var findPipettingClass = function(items) {
		// Pick liquid properties by inspecting source contents
		const pipettingClasses0 = items.map(item => {
			const source = _.isArray(item.source) ? item.source : [item.source];
			let pipettingClass = "Water";

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
					var liquids = extractLiquidNamesFromContents(contents);
					var pipettingClasses = _(liquids).map(function(name) {
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
	var findPipettingPosition = function(items) {
		// Pick liquid properties by inspecting source contents
		var sources = _.map(items, 'source');
		var pipettingPositions = _(items).map('destination').map(function(well) {
			var i = well.indexOf('(');
			var labware = well.substr(0, i);
			var wellId = well.substr(i + 1, 3); // FIXME: parse this instead, allow for A1 as well as A01
			var contents = misc.findObjectsValue(labware+".contents."+wellId, data.objects);
			var liquids = extractLiquidNamesFromContents(contents);
			return _.isEmpty(liquids) ? "Dry" : "Wet";
		}).uniq().value();

		if (pipettingPositions.length === 1) {
			return pipettingPositions[0];
		}
		else {
			return null;
		}
	}

	// Assign programs to items
	if (parsed.value.program) {
		_.forEach(items, function(item) {
			if (!item.program)
				item.program = parsed.value.program;
		});
	}
	else {
		function assignProgram(items) {
			// console.log("assignProgram: "+JSON.stringify(items))
			var pipettingClass = findPipettingClass(items);
			if (!pipettingClass) return false;
			var pipettingPosition = findPipettingPosition(items);
			if (!pipettingPosition) return false;
			var tipModels = _(items).map('tipModel').uniq().value();
			if (tipModels.length !== 1) return false;
			const tipModelName = tipModels[0];
			const tipModelCode = misc.getObjectsValue(tipModelName+".programCode", data.objects);
			//console.log({equipment})
			assert(tipModelCode, `missing value for ${tipModelName}.programCode`);
			var program = "\"Roboliq_"+pipettingClass+"_"+pipettingPosition+"_"+tipModelCode+"\"";
			_.forEach(items, function(item) { item.program = program; });
			return true;
		}

		// Try to find program, first for all items
		if (!assignProgram(items)) {
			// Try to find program for each source
			_.forEach(sourceToItems, function(items) {
				if (!assignProgram(items)) {
					// Try to find program for each item for this source
					_.forEach(items, function(item) {
						if (!assignProgram([item])) {
							throw {name: "ProcessingError", message: "could not automatically choose a program for item: "+JSON.stringify(item)};
						}
					});
				}
			});
		}
	}

	// TODO: Limit syringe choices based on params
	var syringesAvailable = _.map(_.keys(equipment.syringe), s => `${equipmentName}.syringe.${s}`) || [];
	var tipModelToSyringes = equipment.tipModelToSyringes;
	// Group the items
	var groups = groupingMethods.groupingMethod3(items, syringesAvailable, tipModelToSyringes);
	// console.log("groups:\n"+JSON.stringify(groups, null, '\t'));

	// Pick syringe for each item
	// For each group assign syringes, starting with the first available one
	_.forEach(groups, function(group) {
		var tipModelToSyringesAvailable = _.cloneDeep(tipModelToSyringes);
		_.forEach(group, function(item) {
			var tipModel = item.tipModel;
			assert(tipModelToSyringesAvailable[tipModel].length >= 1);
			const syringeName = _.get(parsed.objectName, `items.${item.index}.syringe`, item.syringe);
			if (_.isUndefined(syringeName)) {
				item.syringe = tipModelToSyringesAvailable[tipModel].splice(0, 1)[0];
			}
			else {
				item.syringe = syringeName;
			}
		});
	});

	// Pick source well for items, if the source has multiple wells
	// Rotate through source wells in order of max volume
	for (const group of groups) {
		// FIXME: for debug only
		for (const x of group) {
			if (!x.source) {
				console.log({x})
			}
		}
		// ENDFIX
		sourceMethods.sourceMethod3(group, data, effects);
	}

	// Calculate when tips need to be washed
	// Create pipetting commands

	var syringeToSource = {};
	// How clean is the syringe/tip currently?
	var syringeToCleanValue = _.fromPairs(_.map(syringesAvailable, s => [s, 5]));
	var expansionList = [];

	// Create clean commands before pipetting this group
	const createCleanActions = function(syringeToCleanValue) {
		const items = _(syringeToCleanValue).toPairs().filter(([x, n]) => n > 0).map(([syringe, n]) => {
			const syringeNum = _.toNumber(syringe);
			const syringe1 = _.isInteger(syringeNum) ? syringeNum : syringe;
			return {syringe: syringe1, intensity: valueToIntensity[n]};
		}).value();
		if (_.isEmpty(items)) return [];
		return [{
			command: "pipetter.cleanTips",
			agent: agent,
			equipment: equipmentName,
			items
		}];
	}

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
	var syringeToCleanBeginValue = {};
	_.forEach(groups, function(group) {
		_.forEach(group, function(item) {
			var syringe = item.syringe;
			if (!syringeToCleanBeginValue.hasOwnProperty(syringe)) {
				// TODO: handle source's cleanBefore
				var intensity = item.cleanBefore || parsed.value.cleanBegin || parsed.value.clean || "thorough";
				var intensityValue = intensityToValue[intensity];
				syringeToCleanBeginValue[syringe] = intensityValue;
			}
		});
	});
	// Add cleanBegin commands
	expansionList.push.apply(expansionList, createCleanActions(syringeToCleanBeginValue));
	//console.log("expansionList:")
	//console.log(JSON.stringify(expansionList, null, '  '));

	var syringeToCleanAfterValue = {};
	var doCleanBefore = false
	_.forEach(groups, function(group) {
		assert(group.length > 0);
		// What cleaning intensity is required for the tip before aspirating?
		var syringeToCleanBeforeValue = _.clone(syringeToCleanAfterValue);
		//console.log({syringeToCleanBeforeValue, syringeToCleanAfterValue})
		_.forEach(group, function(item) {
			var source = item.source;
			var syringe = item.syringe;
			var isSameSource = (source === syringeToSource[syringe]);

			// Find required clean intensity
			// Priority: max(previousCleanAfter, (item.cleanBefore || params.cleanBetween || params.clean || source.cleanBefore || "thorough"))
			// FIXME: ignore isSameSource if tip has been contaminated by 'Wet' pipetting position
			// FIXME: also take the source's and destination's "cleanBefore" into account
			var intensity = (!isSameSource)
				? item.cleanBefore || parsed.value.cleanBetween || parsed.value.clean || "thorough"
				: item.cleanBefore || parsed.value.cleanBetweenSameSource || parsed.value.cleanBetween || parsed.value.clean || "thorough";
			assert(intensityToValue.hasOwnProperty(intensity));
			var intensityValue = intensityToValue[intensity];
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
			expansionList.push.apply(expansionList, createCleanActions(syringeToCleanBeforeValue));
		}
		doCleanBefore = true;

		var items2 = _.map(group, function(item) {
			return {
				syringe: item.syringe,
				source: item.source,
				destination: item.destination,
				volume: item.volume.format({precision: 14})
			};
		});
		// Pipette
		expansionList.push({
			"command": "pipetter._pipette",
			"agent": agent,
			"equipment": equipmentName,
			"program": group[0].program,
			"items": items2
		});
	});

	// cleanEnd
	// Priority: max(previousCleanAfter, params.cleanEnd || params.clean || "thorough")
	var syringeToCleanEndValue = {};
	//console.log({syringeToCleanValue})
	_.forEach(syringeToCleanValue, function (value, syringe) {
		var intensity = parsed.value.cleanEnd || parsed.value.clean || "thorough";
		assert(intensityToValue.hasOwnProperty(intensity));
		var intensityValue = intensityToValue[intensity];
		if (syringeToCleanAfterValue.hasOwnProperty(syringe))
			intensityValue = Math.max(syringeToCleanAfterValue[syringe], intensityValue);
		if (value < intensityValue)
			syringeToCleanEndValue[syringe] = intensityValue;
	});
	//console.log({syringeToCleanEndValue})
	expansionList.push.apply(expansionList, createCleanActions(syringeToCleanEndValue));

	var expansion = {};
	_.forEach(expansionList, function(cmd, i) {
		expansion[(i+1).toString()] = cmd;
	});

	// Create the effets object
	// TODO: set final tip clean values
	var effects = {};

	return {
		expansion: expansion,
		effects: effects
	};
}

/**
 * Handlers for {@link pipetter} commands.
 * @static
 */
const commandHandlers = {
	"pipetter._aspirate": function(params, parsed, data) {
		//console.log("params", JSON.stringify(params, null, '  '))
		const effects = pipetterUtils.getEffects_pipette(parsed, data);
		//console.log("effects:", JSON.stringify(effects, null, '  '))
		return {effects};
	},
	"pipetter._dispense": function(params, parsed, data) {
		//console.log("params", JSON.stringify(params, null, '  '))
		const effects = pipetterUtils.getEffects_pipette(parsed, data);
		//console.log("effects:", JSON.stringify(effects, null, '  '))
		return {effects};
	},
	"pipetter._pipette": function(params, parsed, data) {
		//console.log("params", JSON.stringify(params, null, '  '))
		//console.log("effects:", JSON.stringify(pipetterUtils.getEffects_pipette(params, data), null, '  '))
		return {
			effects: pipetterUtils.getEffects_pipette(parsed, data)
		};
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
	"pipetter.pipette": pipette,
	"pipetter.pipetteDilutionSeries": function(params, parsed, data) {
		// console.log("pipetter.pipetteDilutionSeries: "+JSON.stringify(parsed))
		const destinationLabware = parsed.objectName.destinationLabware;

		// Fill all destination wells with diluent
		const diluentItems = [];
		const items = [];
		_.forEach(parsed.value.items, (item, itemIndex) => {
			if (_.isEmpty(item.destinations)) return;
			// FIXME: handle `source`
			assert(_.isUndefined(item.source), "`source` property not implemented yet");
			const destination0 = getLabwareWell(destinationLabware, item.destinations[0]);
			const destinations2 = _.tail(item.destinations).map(s => getLabwareWell(destinationLabware, s));
			// console.log({destination0, destinations2})
			let dilutionFactorPrev = 1;

			// get volume of destination0, and use half of it as the final volume
			const volume0 = WellContents.getWellVolume(destination0, data);
			assert(math.compare(volume0, math.unit(0, 'l')) > 0, "first well in dilution series shouldn't be empty");
			const sourceVolume = math.divide(volume0, parsed.value.dilutionFactor);
			const diluentVolume = math.subtract(volume0, sourceVolume);

			const syringeName = parsed.objectName[`items.${itemIndex}.syringe`] || item.syringe;
			//console.log({syringeName})

			// Distribute diluent to all destination wells
			_.forEach(destinations2, (destinationWell, index) => {
				diluentItems.push({layer: index+1, source: parsed.objectName.diluent, destination: getLabwareWell(destinationLabware, destinationWell), volume: diluentVolume.format({precision: 4}), syringe: syringeName});
			});
			// console.log({diluentItems})

			// Pipette the dilutions
			let source = destination0;
			_.forEach(destinations2, (destinationWell, index) => {
				const destination = getLabwareWell(destinationLabware, destinationWell);
				const item2 = _.merge({}, {layer: index+1, source, destination, volume: sourceVolume.format({precision: 4}), syringe: syringeName});
				items.push(item2);
				source = destination;
			});
			/*const source = (firstItemIsSource) ? dilution0.destination : dilution0.source;
			_.forEach(series, dilution => {
				// If the first item doesn't define a source, but it's dilutionFactor = 1, then treat the destination well as the source.
				assert(!_.isUndefined(source), "dilution item requires a source");
				diluentItems.push({source: parsed.objectName.diluent, destination})
			});
			*/
		});

		//const items = [];

		const params1 = _.omit(parsed.orig, ['description', 'diluent', 'items']);
		params1.command = "pipetter.pipette";
		params1.items = diluentItems;
		params1.clean = "none"; // HACK
		const params2 = _.omit(parsed.orig, ['description', 'diluent', 'items']);
		params2.command = "pipetter.pipette";
		params2.items = items;
		// params2.clean = "none"; // HACK
		// params2.cleanEnd = "light"; // HACK
		// console.log({params1, params2})
		return { expansion: { "1": params1, "2": params2 } };
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
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+'/../schemas/pipetter.yaml'),
	commandHandlers: commandHandlers
};
