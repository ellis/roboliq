var _ = require('lodash');
var misc = require('../misc.js');

var objectToPredicateConverters = {
	"Sealer": function(name, object) {
		return {
			value: [{
				"isSealer": {
					"equipment": name
				}
			}]
		};
	},
};

var commandHandlers = {
	"sealer.instruction.run": function(params, data) {
		var effects = {};
		effects[params.object + ".sealed"] = true;
		return {
			effects: effects
		};
	},
	// TODO:
	// - [ ] raise and error if the sealer site is occupied
	// - [ ] raise error if plate's location isn't set
	// - [ ] return result of query for possible alternative settings
	"sealer.action.sealPlate": function(params, data) {
		var llpl = require('../HTN/llpl.js').create();
		llpl.initializeDatabase(data.predicates);

		var agent = params.agent || "?agent";
		var equipment = params.equipment || "?equipment";
		var program = params.program || "?program";
		var site = params.site || "?site";

		var object = expect.objectsValue({}, params.object, data.objects);
		var model = object.model || "?model";

		var query = {
			"sealer.canAgentEquipmentProgramModelSite": {
				"agent": agent,
				"equipment": equipment,
				"program": program,
				"model": model,
				"site": site
			}
		};
		var resultList = llpl.query(query);
		if (_.isEmpty(resultList)) {
			var query2 = {
				"sealer.canAgentEquipmentProgramModelSite": {
					"agent": "?agent",
					"equipment": "?equipment",
					"program": "?program",
					"model": "?model",
					"site": "?site"
				}
			};
			var resultList2 = llpl.query(query2);
			if (_.isEmpty(resultList2)) {
				return {
					errors: ["missing sealer data (please add predicates `sealer.canAgentEquipmentProgramModelSite`)"]
				};
			} else {
				return {
					errors: ["missing sealer configuration for " + JSON.stringify(query)]
				};
			}
		}

		// Find any parameters which can only take one specific value
		var params2 = {};
		var paramValues = misc.extractValuesFromQueryResults(resultList, "sealer.canAgentEquipmentProgramModelSite");
		_.forEach(paramValues, function(values, name) {
			if (values.length == 1) {
				params2[name] = values[0];
			}
		});

		var alternatives = _(resultList).map(_.values).flatten().map(function(params2) {
			var params3 = {};
			_.forEach(['agent', 'equipment', 'program', 'site'], function(name) {
				if (!params.hasOwnProperty(name)) params3[name] = params2[name];
			});
			return params3;
		}).reject(_.isEmpty).value();

		if (!params2.site) {
			return {
				errors: ["`site`: please provide value"],
				alternatives: alternatives
			};
		}

		var params3 = _.merge({}, {
			command: "sealer.instruction.run",
			agent: params2.agent,
			equipment: params2.equipment,
			program: params2.program,
			object: params.object
		});

		var expansion = {
			"1": {
				"command": "transporter.action.movePlate",
				"object": params.object,
				"destination": params2.site
			},
			"2": params3,
			"3": {
				"command": "transporter.action.movePlate",
				"object": params.object,
				"destination": object.location
			},
		};

		// Create the effets object
		var effects = {};
		effects[params.object + ".sealed"] = true;

		return {
			expansion: expansion,
			effects: effects,
			alternatives: alternatives
		};
	}
};

module.exports = {
	objectToPredicateConverters: objectToPredicateConverters,
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
