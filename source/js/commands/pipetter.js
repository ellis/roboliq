var _ = require('lodash');
var assert = require('assert');
var math = require('mathjs');
var misc = require('../misc.js');
var sourceParser = require('../parsers/sourceParser.js');

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

function createEffects_pipette(params, data) {
	var effects = {};

	//console.log(JSON.stringify(params));
	_.forEach(params.items, function(item) {
		//console.log(JSON.stringify(item));
		var volume = math.eval(item.volume);
		var srcInfo = sourceParser.parse(item.source);
		assert(srcInfo.wellId);
		var dstInfo = sourceParser.parse(item.destination);
		assert(dstInfo.wellId);
		var srcContentsName = srcInfo.labware+".contents."+srcInfo.wellId;
		var dstContentsName = dstInfo.labware+".contents."+dstInfo.wellId;
		var srcContents = effects[srcContentsName] || _.cloneDeep(misc.getObjectsValue(data.objects, srcContentsName)) || ["0ul", item.source];
		//console.log("srcContents", srcContents)
		if (!_.isEmpty(srcContents)) {
			// Get destination contents before the command
			var dstContents = effects[dstContentsName] || _.cloneDeep(misc.getObjectsValue(data.objects, dstContentsName));
			if (_.isEmpty(dstContents)) {
				dstContents = ["0ul"];
			}
			//console.log("dstContents", dstContents)
			var dstVolume = math.eval(dstContents[0]);
			// Increase total well volume
			dstContents[0] = math.chain(dstVolume).add(volume).done().format({precision: 14});
			// Create new content element to add to the contents list
			var newContents = (srcContents.length === 2 && _.isString(srcContents[1]))
				? srcContents[1]
				: srcContents;
			//console.log("newContents", newContents)
			// Augment contents list
			dstContents.push(newContents);
			//console.log("dstContents2", dstContents)

			// Decrease volume of source
			var srcVolume = math.eval(srcContents[0]);
			srcContents[0] = math.chain(srcVolume).subtract(volume).done().format({precision: 14});

			// Update effects
			effects[srcContentsName] = srcContents;
			effects[dstContentsName] = dstContents;
		}
	});

	return effects;
}

var commandHandlers = {
	"pipetter.instruction.aspirate": function(params, data) {
		var effects = {};
		return {
			effects: effects
		};
	},
	"pipetter.instruction.cleanTips": function(params, data) {
		var effects = {};
		return {
			effects: effects
		};
	},
	"pipetter.instruction.dispense": function(params, data) {
		var effects = {};
		return {
			effects: effects
		};
	},
	"pipetter.instruction.pipette": function(params, data) {
		return {
			effects: createEffects_pipette(params, data)
		};
	},

	"pipetter.action.cleanTips": function(params, data) {
		if (_.isEmpty(params.syringes))
			return {};
		if (!params.equipment)
			return {errors: ["`equipment` parameter required"]};
		if (!params.intensity)
			return {errors: ["`intensity` parameter required"]};

		var llpl = require('../HTN/llpl.js');
		llpl.initializeDatabase(data.predicates);

		var agent = params.agent || "?agent";
		var equipment = params.equipment;
		var program = params.program || "?program";
		var intensity = params.intensity

		// Check whether labwares are on sites that can be pipetted
		var syringeToProgram_l = _.map(params.syringes, function(syringe) {
			var tipModelRef = equipment+".syringes."+syringe+".tipModel";
			var tipModel = misc.getObjectsValue(data.objects, tipModelRef);
			if (!tipModel) {
				return {errors: [tipModelRef+" must be set"]};
			}
			var query = {
				"pipetter.cleanTips.canAgentEquipmentProgramModelIntensity": {
					"agent": agent,
					"equipment": equipment,
					"program": program,
					"model": tipModel,
					"intensity": intensity
				}
			};
			//console.log("query: "+JSON.stringify(query, null, '\t'));
			var queryResults = llpl.query(query);
			//console.log("queryResults: "+JSON.stringify(queryResults, null, '\t'));
			if (_.isEmpty(queryResults)) {
				throw {name: "ProcessingError", errors: ["no wash program defined for "+equipment+" with tip model "+tipModel+" and intensity "+intensity]};
			}
			var x = queryResults[0]["pipetter.cleanTips.canAgentEquipmentProgramModelIntensity"];
			agent = x.agent
			//console.log("x: "+JSON.stringify(x, null, '\t'));
			return {syringe: syringe, program: x.program};
		});

		var expansion_l = [];
		while (!_.isEmpty(syringeToProgram_l)) {
			var program = syringeToProgram_l[0].program;
			var l = _.remove(syringeToProgram_l, function(x) { return x.program === program; });
			var syringe_l = _.map(l, 'syringe');
			expansion_l.push({
				command: "pipetter.instruction.cleanTips",
				agent: agent,
				equipment: equipment,
				program: program,
				syringes: syringe_l
			});
		}

		var expansion = {};
		_.forEach(expansion_l, function(cmd, i) {
			expansion[(i+1).toString()] = cmd;
		});

		// Create the effects object
		var effects = {};

		return {
			expansion: expansion,
			effects: effects
		};
	},
	"pipetter.action.pipette": function(params, data) {
		var llpl = require('../HTN/llpl.js');
		llpl.initializeDatabase(data.predicates);

		var items = _.cloneDeep(params.items);
		var agent = params.agent || "?agent";
		var equipment = params.equipment || "?equipment";
		var cleanBefore = params.cleanBefore || params.clean || "thorough";
		var cleanBetween = params.cleanBetween || params.clean || "thorough";
		var cleanBetweenSameSource = params.cleanBetweenSameSource || cleanBetween;
		var cleanAfter = params.cleanAfter || params.clean || "thorough";
		//var tipModels = params.tipModels;
		//var syringes = params.syringes;

		// Find all wells, both sources and destinations
		var wellName_l = _(items).map(function (item) {
			// TODO: allow source to refer to a set of wells, not just a single well
			// TODO: create a function getSourceWells()
			return [item.source, item.destination]
		}).flatten().value();

		// Find all labware
		var labwareName_l = _(wellName_l).map(function (wellName) {
			var i = wellName.indexOf('(');
			// TODO: handle case where source is a source object rather than a well
			return (i >= 0) ? wellName.substr(0, i) : wellName;
		}).uniq().value();
		var labware_l = _.map(labwareName_l, function (name) { return _.merge({name: name}, misc.getObjectsValue(data.objects, name)); });

		// Check whether labwares are on sites that can be pipetted
		var query2_l = [];
		_.forEach(labware_l, function(labware) {
			if (!labware.location) {
				return {errors: [labware.name+".location must be set"]};
			}
			var query = {
				"pipetter.canAgentEquipmentSite": {
					"agent": agent,
					"equipment": equipment,
					"site": labware.location
				}
			};
			var queryResults = llpl.query(query);
			//console.log("queryResults: "+JSON.stringify(queryResults, null, '\t'));
			if (_.isEmpty(queryResults)) {
				throw {name: "ProcessingError", errors: [labware.name+" is at a site "+labware.location+", which hasn't been configured for pipetting; please move to a pipetting site."]};
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
				equipment = x.equipment;
			}
		}

		// TODO: if labwares are not on sites that can be pipetted, try to move them to appropriate sites

		// Try to find a tipModel for the given items
		var findTipModel = function(items) {
			var canUse1000 = true;
			var canUse0050 = true;
			var pos = "wet";
			_.forEach(items, function(item) {
				var volume = math.eval(item.volume);
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
			var pipettingClasses = _(items).map('source').map(function(source) {
				var pipettingClass = "Water";
				var i = source.indexOf('(');
				var well = null;
				// If the source is a source name
				if (i < 0) {
					var wells = misc.getObjectsValue(data.objects, source+".wells");
					if (!_.isEmpty(wells))
						well = wells[0];
				}
				// Else
				else {
					well = source;
				}

				if (well) {
					var labware = source.substr(0, i);
					var wellId = source.substr(i + 1, 3); // FIXME: parse this instead, allow for A1 as well as A01
					var contents = misc.getObjectsValue(data.objects, labware+".contents."+wellId);
					var liquids = extractLiquidNamesFromContents(contents);
					var pipettingClasses = _(liquids).map(function(name) {
						return misc.getObjectsValue(data.objects, name+".pipettingClass") || "Water";
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

				return pipettingClass;
			}).uniq().value();

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
				var contents = misc.getObjectsValue(data.objects, labware+".contents."+wellId);
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

		var assignProgram = function(items) {
			var pipettingClass = findPipettingClass(items);
			if (!pipettingClass) return false;
			var pipettingPosition = findPipettingPosition(items);
			if (!pipettingPosition) return false;
			var tipModels = _(items).map('tipModel').uniq().value();
			if (tipModels.length !== 1) return false;
			var tipModel = tipModels[0];
			if (tipModel.indexOf("0050") >= 0) tipModelCode = "0050";
			else tipModelCode = "1000";
			var program = "\"Roboliq_"+pipettingClass+"_"+pipettingPosition+"_"+tipModelCode+"\"";
			_.forEach(items, function(item) { item.program = program; });
			return true;
		}

		// FIXME: allow for overriding program via params
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

		// FIXME: limit syringe choices based on params
		// TODO: Pick syringe for each item
		var tipModelToSyringes = {
			"ourlab.mario.tipModel1000": [1, 2, 3, 4],
			"ourlab.mario.tipModel0050": [5, 6, 7, 8]
		};
		var tipModelToSyringesAvailable = {};
		// Method 2: Use each tip, rotating through them
		_.forEach(items, function(item) {
			var tipModel = item.tipModel;
			if (_.isEmpty(tipModelToSyringesAvailable[tipModel])) {
				tipModelToSyringesAvailable[tipModel] = _.clone(tipModelToSyringes[tipModel]);
			}
			//console.log("A", tipModel, tipModelToSyringesAvailable)
			assert(tipModelToSyringesAvailable[tipModel].length >= 1);
			var syringe = tipModelToSyringesAvailable[tipModel][0];
			tipModelToSyringesAvailable[tipModel] = _.tail(tipModelToSyringesAvailable[tipModel]);
			item.syringe = syringe;
		});

		// TODO: pick source well for items, if the source has multiple wells
		// For now, pick the first well in a source set and ignore the others
		_.forEach(items, function (item) {
			var source = item.source;
			var sourceInfo = sourceParser.parse(item.source);
			if (sourceInfo.source) {
				var wells = getObjectsValue(data.objects, source+".wells");
				assert(!_.isEmpty(wells));
				item.sourceWell = wells[0];
			}
			else {
				item.sourceWell = source;
			}
		});

		// Calculate when tips need to be washed
		// Create pipetting commands

		var syringeToSource = {};
		// How clean is the syringe/tip currently?
		var syringeToCleanValue = {"1": 5, "2": 5, "3": 5, "4": 5, "6": 5, "7": 5, "8": 5};
		// What cleaning intensity is required for tip cleaning before the very first aspiration?
		var syringeToCleanValueBegin = {};
		// What cleaning intensity is required for the tip before aspirating?
		var syringeToCleanValueBeforeAspirate = {};
		var expansionList = [];
		var program = null;

		// Create clean commands before pipetting this group
		var createCleanActions = function(syringeToCleanValue) {
			var expansionList = [];

			if (_.isEmpty(syringeToCleanValue)) return [];
			var l1000 = _.compact([
				syringeToCleanValue[1],
				syringeToCleanValue[2],
				syringeToCleanValue[3],
				syringeToCleanValue[4]
			]);
			var l0050 = _.compact([
				syringeToCleanValue[5],
				syringeToCleanValue[6],
				syringeToCleanValue[7],
				syringeToCleanValue[8]
			]);
			if (!_.isEmpty(l1000)) {
				var value = _.max(l1000);
				var intensity = valueToIntensity[value];
				var syringes = [1, 2, 3, 4];
				expansionList.push({
					command: "pipetter.action.cleanTips",
					agent: agent,
					equipment: equipment,
					intensity: intensity,
					syringes: syringes
				});
				_.forEach(syringes, function(syringe) { syringeToCleanValue[syringe] = value; });
			}
			if (!_.isEmpty(l0050)) {
				var value = _.max(l0050);
				var intensity = valueToIntensity[value];
				var syringes = [5, 6, 7, 8];
				expansionList.push({
					command: "pipetter.action.cleanTips",
					agent: agent,
					equipment: equipment,
					intensity: intensity,
					syringes: syringes
				});
				_.forEach(syringes, function(syringe) { syringeToCleanValue[syringe] = value; });
			}
			return expansionList;
		}

		// Method 2: Use each tip, rotating through them till they need to be washed
		var items2 = items;
		while (!_.isEmpty(items2)) {
			var n = items2.length;
			for (var i = 0; i < items2.length; i++) {
				var item = items2[i];
				// Make sure all items in the group use the same program
				if (i == 0) {
					program = item.program;
				}
				else if (program !== item.program) {
					break;
				}

				var source = item.source;
				var syringe = item.syringe;
				var isSameSource = (source === syringeToSource[syringe]);

				// Find required clean intensity
				// FIXME: ignore isSameSource if tip has been contaminated by 'Wet' pipetting position
				// FIXME: also take the source's and destination's "cleanBefore" and "cleanAfter" properties into account
				var intensity = (isSameSource) ? cleanBetweenSameSource : cleanBetween;
				var intensityValue = intensityToValue[intensity];

				// If the tip needs to be cleaned before aspiration,
				if (intensityValue > syringeToCleanValue[syringe]) {
					n = i;
					break;
				}

				// Update cleaning value required before first aspirate
				if (!syringeToCleanValueBegin.hasOwnProperty(syringe)) {
					syringeToCleanValueBegin[syringe] = intensityValue;
				}
				// Update cleaning value required before current aspirate
				if (!syringeToCleanValueBeforeAspirate.hasOwnProperty(syringe) || intensityValue > syringeToCleanValueBeforeAspirate[syringe]) {
					syringeToCleanValueBeforeAspirate[syringe] = intensityValue;
				}

				// Set the aspirated source and indicate the the tip is no longer clean
				syringeToSource[syringe] = source;
				syringeToCleanValue[syringe] = 0;
				// TODO: if wet contact, indicate tip contanmination
			}

			// Select items for the pipetting instruction
			var items3 = _.take(items2, n);
			items2 = _.drop(items2, n);

			// Add clean commands before pipetting this group, but only if it isn't the first group in the list
			if (!_.isEmpty(expansionList)) {
				expansionList.push.apply(expansionList, createCleanActions(syringeToCleanValueBeforeAspirate));
			}

			var items4 = _.map(items3, function(item) {
				return {
					syringe: item.syringe,
					source: item.sourceWell,
					destination: item.destination,
					volume: item.volume
				};
			});
			// Pipette
			expansionList.push({
				"command": "pipetter.instruction.pipette",
				"agent": agent,
				"equipment": equipment,
				"program": program,
				"items": items4
			});
		}

		// Clean after
		var cleanAfterValue = intensityToValue[cleanAfter];
		var syringeToCleanValueAfter = {};
		_.forEach(syringeToCleanValue, function (value, syringe) {
			if (value < cleanAfterValue)
				syringeToCleanValueAfter[syringe] = cleanAfterValue;
		});
		// Add clean commands before pipetting this group
		expansionList.push.apply(expansionList, createCleanActions(expansionList, createCleanActions(syringeToCleanValueBeforeAspirate)));

		// Clean before: prepend actions to the expansion list
		expansionList = createCleanActions(syringeToCleanValueBegin).concat(expansionList);

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
};

module.exports = {
	//objectToPredicateConverters: objectToPredicateConverters,
	commandHandlers: commandHandlers
};
