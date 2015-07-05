var _ = require('lodash');
var misc = require('../misc.js');

var commandHandlers = {
	"pipetter.instruction.aspirate": function(params, objects) {
		var effects = {};
		return {
			effects: effects
		};
	},
	"pipetter.instruction.cleanTips": function(params, objects) {
		var effects = {};
		return {
			effects: effects
		};
	},
	"pipetter.instruction.dispense": function(params, objects) {
		var effects = {};
		return {
			effects: effects
		};
	},
	"pipetter.instruction.pipette": function(params, objects) {
		var effects = {};
		return {
			effects: effects
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

		var agent = params.agent || "?agent";
		var equipment = params.equipment || "?equipment";
		var cleanBefore = params.cleanBefore || params.clean || "thorough";
		var cleanBetween = params.cleanBetween || params.clean || "thorough";
		var cleanBetweenSameSource = params.cleanBetweenSameSource || cleanBetween;
		var cleanAfter = params.cleanAfter || params.clean || "thorough";
		var tipModels = params.tipModels;
		var syringes = params.syringes;

		// Find all wells, both sources and destinations
		var wellName_l = _(params.items).map(function (item) {
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

		// TODO: pick program
		var program = params.program || "Water";

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

		_.forEach(params.items, function (item, i) {
			var source = item.source;
			var syringe = 1;
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
				"program": program,
				"items": [_.merge({syringe: syringe}, item)]
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
