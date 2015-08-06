var _ = require('lodash');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
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
		//console.log("params:\n"+JSON.stringify(params, null, '  '))
		var parsed = commandHelper.parseParams(params, data, {
			agent: "name?",
			equipment: "name?",
			program: "name?",
			object: "name",
			site: "name?",
			destinationAfter: "name?"
		});
		//console.log("parsed:\n"+JSON.stringify(parsed, null, '  '))
		var model = commandHelper.getParsedValue(parsed, data, 'object', 'model');
		var location0 = commandHelper.getParsedValue(parsed, data, 'object', 'location');

		var predicates = [
			{"sealer.canAgentEquipmentProgramModelSite": {
				"agent": parsed.agent.valueName,
				"equipment": parsed.equipment.valueName,
				"program": parsed.program.valueName,
				"model": model,
				"site": parsed.site.valueName
			}}
		];
		var alternatives = commandHelper.queryLogic(data, predicates, '[].and[]."sealer.canAgentEquipmentProgramModelSite"');
		var params2 = alternatives[0];
		//console.log("params2:\n"+JSON.stringify(params2, null, '  '))

		var params3 = _.merge({}, {
			command: "sealer.instruction.run",
			agent: params2.agent,
			equipment: params2.equipment,
			program: params2.program,
			object: params.object
		});

		var expansion = {
			"1": {
				"command": "transporter.movePlate",
				"object": parsed.object.valueName,
				"destination": params2.site
			},
			"2": params3,
			"3": {
				"command": "transporter.movePlate",
				"object": parsed.object.valueName,
				"destination": location0
			},
		};

		// Create the effects object
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
