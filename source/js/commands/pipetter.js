var _ = require('lodash');
var assert = require('assert');
var math = require('mathjs');
var expect = require('../expect.js');
var misc = require('../misc.js');
var groupingMethods = require('./pipetter/groupingMethods.js');
var pipetterUtils = require('./pipetter/pipetterUtils.js');
var sourceMethods = require('./pipetter/sourceMethods.js');
var sourceParser = require('../parsers/sourceParser.js');
var wellsParser = require('../parsers/wellsParser.js');

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

// REFACTOR: remove this variable
var createEffects_pipette = pipetterUtils.getEffects_pipette;

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
		console.log("effects:", JSON.stringify(createEffects_pipette(params, data), null, '  '))
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

		var items = (params.items) ? _.cloneDeep(params.items) : [];
		var agent = params.agent || "?agent";
		var equipment = params.equipment || "?equipment";
		//var tipModels = params.tipModels;
		//var syringes = params.syringes;

		var sourcesTop = [];
		if (params.sources) {
			expect.try({paramName: 'sources'}, function () {
				if (_.isString(params.sources))
					sourcesTop = wellsParser.parse(params.sources, data.objects);
				else
					sourcesTop = params.sources;
			});
		}

		var destinationsTop = [];
		if (params.destinations) {
			expect.try({paramName: 'destinations'}, function () {
				if (_.isString(params.destinations))
					destinationsTop = wellsParser.parse(params.destinations, data.objects);
				else
					destinationsTop = params.destinations;
			});
			//console.log("destinationsTop:", destinationsTop)
			//console.log(destinationsTop.length)
		}

		var volumesTop = [];
		if (params.volumes) {
			if (_.isString(params.volumes))
				volumesTop = [params.volumes];
			else
				volumesTop = params.volumes;
		}

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
				if (!items[i].source) {
					if (sourcesTop.length == 1)
						items[i].source = sourcesTop[0];
					else if (sourcesTop.length > 1)
						items[i].source = sourcesTop[i];
				}
				if (!items[i].destination) {
					//console.log("step", items[i], destinationsTop, i, destinationsTop[i])
					if (destinationsTop.length == 1)
						items[i].destination = destinationsTop[0];
					else if (destinationsTop.length > 1)
						items[i].destination = destinationsTop[i];
				}
				if (!items[i].volume) {
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
		wellName_l = _.compact(_.flattenDeep([wellName_l, sourcesTop, destinationsTop]));

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

		var sourceToItems = _.groupBy(items, 'source');

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
					var wells = misc.findObjectsValue(data.objects, source+".wells");
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
					var contents = misc.findObjectsValue(data.objects, labware+".contents."+wellId);
					var liquids = extractLiquidNamesFromContents(contents);
					var pipettingClasses = _(liquids).map(function(name) {
						return misc.findObjectsValue(data.objects, name+".pipettingClass", "Water");
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
				var contents = misc.findObjectsValue(data.objects, labware+".contents."+wellId);
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
		if (params.program) {
			_.forEach(items, function(item) {
				if (!item.program)
					item.program = params.program;
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
				if (tipModel.indexOf("0050") >= 0) tipModelCode = "0050";
				else tipModelCode = "1000";
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
		// TODO: get syringe information from data.objects
		var syringesAvailable = params.syringes || [1, 2, 3, 4, 5, 6, 7, 8];
		var tipModelToSyringes = {
			"ourlab.mario.tipModel1000": [1, 2, 3, 4],
			"ourlab.mario.tipModel0050": [5, 6, 7, 8]
		};
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
		_.forEach(groups, function(group) {
			sourceMethods.sourceMethod3(group, data, effects);
		});

		// Calculate when tips need to be washed
		// Create pipetting commands

		var syringeToSource = {};
		// How clean is the syringe/tip currently?
		var syringeToCleanValue = {"1": 5, "2": 5, "3": 5, "4": 5, "6": 5, "7": 5, "8": 5};
		var expansionList = [];

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

			var sub = function(value, syringes) {
				if (value > 0) {
					var intensity = valueToIntensity[value];
					expansionList.push({
						command: "pipetter.action.cleanTips",
						agent: agent,
						equipment: equipment,
						intensity: intensity,
						syringes: syringes
					});
					_.forEach(syringes, function(syringe) { syringeToCleanValue[syringe] = value; });
				}
			}
			if (!_.isEmpty(l1000)) {
				sub(_.max(l1000), [1, 2, 3, 4]);
			}
			if (!_.isEmpty(l0050)) {
				sub(_.max(l0050), [5, 6, 7, 8]);
			}
			return expansionList;
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

		//var cleanBefore = params.cleanBefore || params.clean || "thorough";
		//var cleanBetween = params.cleanBetween || params.clean || "thorough";
		//var cleanBetweenSameSource = params.cleanBetweenSameSource || cleanBetween;
		//var cleanAfter = params.cleanAfter || params.clean || "thorough";

		// Find the cleaning intensity required before the first aspiration
		var syringeToCleanBeginValue = {};
		_.forEach(groups, function(group) {
			_.forEach(group, function(item) {
				var syringe = item.syringe;
				if (!syringeToCleanBeginValue.hasOwnProperty(syringe)) {
					// TODO: handle source's cleanBefore
					var intensity = item.cleanBefore || params.cleanBegin || params.clean || "thorough";
					var intensityValue = intensityToValue[intensity];
					syringeToCleanBeginValue[syringe] = intensityValue;
				}
			});
		});
		// Add cleanBegin commands
		expansionList.push.apply(expansionList, createCleanActions(syringeToCleanBeginValue));

		var syringeToCleanAfterValue = {};
		var doCleanBefore = false
		_.forEach(groups, function(group) {
			assert(group.length > 0);
			// What cleaning intensity is required for the tip before aspirating?
			var syringeToCleanBeforeValue = _.clone(syringeToCleanAfterValue);
			_.forEach(group, function(item) {
				var source = item.source;
				var syringe = item.syringe;
				var isSameSource = (source === syringeToSource[syringe]);

				// Find required clean intensity
				// Priority: max(previousCleanAfter, (item.cleanBefore || params.cleanBetween || params.clean || source.cleanBefore || "thorough"))
				// FIXME: ignore isSameSource if tip has been contaminated by 'Wet' pipetting position
				// FIXME: also take the source's and destination's "cleanBefore" into account
				var intensity = (!isSameSource)
					? item.cleanBefore || params.cleanBetween || params.clean || "thorough"
					: item.cleanBefore || params.cleanBetweenSameSource || params.cleanBetween || params.clean || "thorough";
				assert(intensityToValue.hasOwnProperty(intensity));
				var intensityValue = intensityToValue[intensity];
				if (syringeToCleanAfterValue.hasOwnProperty(syringe))
					intensityValue = Math.max(syringeToCleanAfterValue[syringe], intensityValue);

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
				"program": group[0].program,
				"items": items2
			});
		});

		// cleanEnd
		// Priority: max(previousCleanAfter, params.cleanEnd || params.clean || "thorough")
		var syringeToCleanEndValue = {};
		_.forEach(syringeToCleanValue, function (value, syringe) {
			var intensity = params.cleanEnd || params.clean || "thorough";
			assert(intensityToValue.hasOwnProperty(intensity));
			var intensityValue = intensityToValue[intensity];
			if (syringeToCleanAfterValue.hasOwnProperty(syringe))
				intensityValue = Math.max(syringeToCleanAfterValue[syringe], intensityValue);
			if (value < intensityValue)
				syringeToCleanEndValue[syringe] = intensityValue;
		});
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
};

module.exports = {
	//objectToPredicateConverters: objectToPredicateConverters,
	commandHandlers: commandHandlers
};
