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

	"pipetter.action.cleanTips": function(params, objects, predicates, planHandlers) {
		if (_.isEmpty(params.syringes))
			return {};
		if (!params.equipment)
			return {errors: ["`equipment` parameter required"]};
		if (!params.intensity)
			return {errors: ["`intensity` parameter required"]};

		var llpl = require('../HTN/llpl.js');
		llpl.initializeDatabase(predicates);

		var agent = params.agent || "?agent";
		var equipment = params.equipment;
		var program = params.program || "?program";
		var intensity = params.intensity

		// Check whether labwares are on sites that can be pipetted
		var syringeToProgram_l = _.map(params.syringes, function(syringe) {
			var tipModelRef = equipment+".syringes."+syringe+".tipModel";
			var tipModel = misc.getObjectsValue(objects, tipModelRef);
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
	"pipetter.action.pipette": function(params, objects, predicates, planHandlers) {
		var llpl = require('../HTN/llpl.js');
		llpl.initializeDatabase(predicates);

		var agent = params.agent || "?agent";
		var equipment = params.equipment || "?equipment";

		// Find all wells, both sources and destinations
		var wellName_l = _(params.items).map(function (item) {
			return [item.source, item.destination]
		}).flatten().value();

		// Find all labware
		var labwareName_l = _(wellName_l).map(function (wellName) {
			var i = wellName.indexOf('(');
			return (i >= 0) ? wellName.substr(0, i) : wellName;
		}).uniq().value();
		var labware_l = _.map(labwareName_l, function (name) { return _.merge({name: name}, misc.getObjectsValue(objects, name)); });

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
				return {errors: [labware.name+" is at a site "+labware.location+", which hasn't been configured for pipetting; please move to a pipetting site."]}
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
		var program = "Water";

		// TODO: calculate when tips need to be washed

		// Method 1: only use one tip

		var items = _(params.items).map(function (item) {
			return _.merge({syringe: 1}, item);
		}).flatten().value();

		var expansion = {
			"1": {
				"command": "pipetter.instruction.pipette",
				"agent": agent,
				"equipment": equipment,
				"program": program,
				"items": items
			}
		};

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

/*

  {"method": {"description": "sealer.sealPlate-null: plate already sealed",
    "task": {"sealer.sealPlate": {"labware": "?labware"}},
    "preconditions": [
      {"plateIsSealed": {"labware": "?labware"}}
    ],
    "subtasks": {"ordered": [
      {"trace": {"text": "sealer.sealPlate-null"}}
    ]}
  }},

  {"method": {"description": "method for sealing",
    "task": {"sealer.sealPlate": {"labware": "?labware"}},
    "preconditions": [
      {"model": {"labware": "?labware", "model": "?model"}},
      {"sealer.canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?site"}}
    ],
    "subtasks": {"ordered": [
      {"ensureLocation": {"labware": "?labware", "site": "?site"}},
      {"sealAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "site": "?site"}}
    ]}
  }}
*/
