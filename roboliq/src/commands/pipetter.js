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

function pipette(params, parsed, data) {
	var llpl = require('../HTN/llpl.js').create();
	llpl.initializeDatabase(data.predicates);

	//console.log("pipette: "+JSON.stringify(parsed))

	let items = (_.isUndefined(parsed.value.items))
		? []
		: parsed.value.items.map(parsedItem =>
				_(parsedItem).map((value, name) => [name, value]).filter(l => !_.isUndefined(l[1])).zipObject().value()
			);
	//console.log("items: "+JSON.stringify(items));
	let agent = parsed.objectName.agent || "?agent";
	let equipmentName = parsed.objectName.equipment || "?equipment";
	//var tipModels = params.tipModels;
	//var syringes = params.syringes;

	const sourcesTop = parsed.value.sources || [];
	//console.log({sourcesTop})
	const destinationsTop = parsed.value.destinations || [];
	const volumesTop = parsed.value.volumes || [];

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
			//console.log("i:", i)
			if (_.isUndefined(items[i].source)) {
				if (sourcesTop.length == 1)
					items[i].source = sourcesTop[0];
				else if (sourcesTop.length > 1)
					items[i].source = sourcesTop[i];
			}

			if (_.isUndefined(items[i].destination)) {
				//console.log("step", items[i], destinationsTop, i, destinationsTop[i])
				if (destinationsTop.length == 1)
					items[i].destination = destinationsTop[0];
				else if (destinationsTop.length > 1)
					items[i].destination = destinationsTop[i];
			}
			if (_.isUndefined(items[i].volume)) {
				if (volumesTop.length == 1)
					items[i].volume = volumesTop[0];
				else if (volumesTop.length > 1)
					items[i].volume = volumesTop[i];
			}
		}
	}
	//console.log(JSON.stringify(sourcesTop, null, '  '))
	//console.log(JSON.stringify(items, null, '  '))

	// Find all wells, both sources and destinations
	var wellName_l = _(items).map(function (item) {
		// TODO: allow source to refer to a set of wells, not just a single well
		// TODO: create a function getSourceWells()
		return [item.source, item.destination]
	}).flatten().compact().value();
	wellName_l = _.uniq(_.compact(_.flattenDeep([wellName_l, sourcesTop, destinationsTop])));
	//console.log("wellName_l", JSON.stringify(wellName_l))

	// Find all labware
	var labwareName_l = _(wellName_l).map(function (wellName) {
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
	var findTipModel = function(items) {
		var canUse1000 = true;
		var canUse0050 = true;
		var pos = "wet";
		_.forEach(items, function(item) {
			//console.log("item:", item)
			var volume = item.volume;
			assert.equal(volume.unit.name, 'l', "expected units to be in liters");
			if (math.compare(volume, math.eval("0.25ul")) < 0 || math.compare(volume, math.eval("45ul")) > 0) {
				canUse0050 = false;
			}
			if (math.compare(volume, math.eval("3ul")) < 0) {
				canUse1000 = false;
			}
		});

		if (canUse1000 || canUse0050) {
			var tipModel = (canUse1000) ? "ourlab.mario.tipModel1000" : "ourlab.mario.tipModel0050";
			_.forEach(items, function(item) {
				if (!item.tipModel) item.tipModel = tipModel;
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
	items = _.filter(items, item => item.volume.toNumber('l') > 0);

	// Try to find tipModel, first for all items
	if (!findTipModel(items)) {
		// Try to find tipModel for each source
		_.forEach(sourceToItems, function(items) {
			if (!findTipModel(items)) {
				// Try to find tipModel for each item for this source
				_.forEach(items, function(item) {
					if (!findTipModel([item])) {
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
			// ENDIFX

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
					if (!pipettingClasses.indexOf("Water") >= 0) {
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
		var assignProgram = function(items) {
			var pipettingClass = findPipettingClass(items);
			if (!pipettingClass) return false;
			var pipettingPosition = findPipettingPosition(items);
			if (!pipettingPosition) return false;
			var tipModels = _(items).map('tipModel').uniq().value();
			if (tipModels.length !== 1) return false;
			var tipModel = tipModels[0];
			var tipModelCode = (tipModel.indexOf("0050") >= 0)
				? tipModelCode = "0050"
				: tipModelCode = "1000";
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

	// Limit syringe choices based on params
	var syringesAvailable = params.syringes || _.map(_.keys(equipment.syringe), s => `${equipmentName}.syringe.${s}`) || [];
	var tipModelToSyringes = equipment.tipModelToSyringes;
	// Group the items
	var groups = groupingMethods.groupingMethod2(items, syringesAvailable, tipModelToSyringes);
	//console.log("groups:\n"+JSON.stringify(groups, null, '\t'));

	// Pick syringe for each item
	// For each group assign syringes, starting with the first available one
	_.forEach(groups, function(group) {
		var tipModelToSyringesAvailable = _.cloneDeep(tipModelToSyringes);
		_.forEach(group, function(item) {
			var tipModel = item.tipModel;
			assert(tipModelToSyringesAvailable[tipModel].length >= 1);
			item.syringe = tipModelToSyringesAvailable[tipModel].splice(0, 1)[0];
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
	var syringeToCleanValue = _.zipObject(_.map(syringesAvailable, s => [s, 5]));
	var expansionList = [];

	// Create clean commands before pipetting this group
	const createCleanActions = function(syringeToCleanValue) {
		const items = _(syringeToCleanValue).pairs().filter(([x, n]) => n > 0).map(([syringe, n]) => { return {syringe, intensity: valueToIntensity[n]} }).value();
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
		const effects = pipetterUtils.getEffects_aspirate(parsed, data);
		//console.log("effects:", JSON.stringify(effects, null, '  '))
		return {effects};
	},
	"pipetter._dispense": function(params, parsed, data) {
		return {};
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
		//console.log("pipetter.cleanTips:")
		//console.log(JSON.stringify(params, null, '\t'));
		//console.log(JSON.stringify(parsed, null, '\t'));

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

		// Get list of valid agent/equipment/syringe combinations for all syringes
		const nodes = _.flatten(items.map(item => {
			const predicates = [
				{"pipetter.canAgentEquipmentSyringe": {
					"agent": parsed.objectName.agent,
					"equipment": parsed.objectName.equipment,
					"syringe": item.syringe
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
		//console.log(syringeToNodes);

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
			//console.log({syringe, nodes})
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
	"pipetter.pipetteMixtures": function(params, parsed, data) {
		const mixtures0 = parsed.value.mixtures;
		const destinations = parsed.value.destinations;
		//console.log("params:", params);
		//console.log("data.objects.mixtures:", data.objects.mixtures);
		//console.log("mixtures:\n"+JSON.stringify(mixtures0));
		//console.log("A:", misc.getVariableValue(params.destinations, data.objects))
		//console.log("data.objects.mixtureWells:", data.objects.mixtureWells);
		//console.log("destinations:", destinations);
		expect.truthy({}, destinations.length >= params.mixtures.length, "length of destinations array must be equal or greater than length of mixtures array.");

		var params2 = _.omit(params, ['mixtures', 'destinations', 'order']);
		// Put 'index' on all non-null mixture subitems
		_.forEach(mixtures0, function(mixture) {
			_.forEach(mixture, function(subitem, index) {
				if (!_.isEmpty(subitem))
					subitem.index = index;
			});
		});
		const mixtures = _.compact(mixtures0);
		//console.log("mixtures:", mixtures);

		params2.items = [];
		_.forEach(_.zip(mixtures, destinations), function(pair) {
			var subitems = pair[0];
			_.forEach(_.compact(subitems), function(subitem, index) {
				params2.items.push(_.merge({index: index}, subitem, {destination: pair[1]}));
			});
		});
		let order = parsed.value.order || ["index"];
		if (!_.isArray(order)) order = [order];
		const ordering = _.map(order, function(what) {
			switch (what) {
				case 'index': return 'index';
				case 'destination': return function(item) { destinations.indexOf(item.destination); };
				default: assert(false, "unknown `order` criterion: "+what);
			}
		});
		//console.log("A:", params2.items)
		params2.items = _.sortByAll.apply(null, [params2.items, ordering]);
		//console.log("B:", params2.items)
		params2.items = _.map(params2.items, function(item) { return _.omit(item, 'index'); });
		//console.log("C:", params2.items)
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
