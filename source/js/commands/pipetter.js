var _ = require('lodash');
var assert = require('assert');
var math = require('mathjs');
var misc = require('../misc.js');
var sourceParser = require('../parsers/sourceParser.js');


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
			dstContents[0] = math.chain(dstVolume).add(volume).done().toString();
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
			srcContents[0] = math.chain(srcVolume).subtract(volume).done().toString();

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
				var syringe = (canUse1000) ? 1 : 5;
				var tipModel = (canUse1000) ? "ourlab.mario.tipModel1000" : "ourlab.mario.tipModel0050";
				_.forEach(items, function(item) {
					if (!item.tipModel) item.tipModel = tipModel;
					if (!item.syringe) item.syringe = syringe;
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

		// TODO: pick sources and syringes for items

		// TODO: calculate when tips need to be washed

		// Method 1: only use one tip

		var expansionList = [];

		// Clean before
		if (cleanBefore !== "none") {
			expansionList.push({
				command: "pipetter.action.cleanTips",
				agent: agent,
				equipment: equipment,
				intensity: cleanBefore,
				syringes: [1]
			});
		}

		var syringeToSource = {};

		_.forEach(items, function (item, i) {
			var source = item.source;
			var syringe = item.syringe;
			var isSameSource = (source === syringeToSource[syringe.toString()]);

			// Clean between
			if (i > 0) {
				var intensity = (isSameSource) ? cleanBetweenSameSource : cleanBetween;
				if (intensity !== "none") {
					expansionList.push({
						command: "pipetter.action.cleanTips",
						agent: agent,
						equipment: equipment,
						intensity: cleanBetween,
						syringes: [syringe]
					});
				}
			}

			// Pipette
			expansionList.push({
				"command": "pipetter.instruction.pipette",
				"agent": agent,
				"equipment": equipment,
				"program": item.program,
				"items": [_.pick(_.merge({syringe: syringe}, item), ["syringe", "source", "destination", "volume"])]
			});
		});

		// Clean after
		if (cleanAfter !== "none") {
			expansionList.push({
				command: "pipetter.action.cleanTips",
				agent: agent,
				equipment: equipment,
				intensity: cleanAfter,
				syringes: [1]
			});
		}

		var expansion = {};
		_.forEach(expansionList, function(cmd, i) {
			expansion[(i+1).toString()] = cmd;
		});

		// Create the effets object
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
