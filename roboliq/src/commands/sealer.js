var _ = require('lodash');
import yaml from 'yamljs';
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
	// TODO:
	// - [ ] raise and error if the sealer site is occupied
	// - [ ] raise error if plate's location isn't set
	// - [ ] return result of query for possible alternative settings
	"sealer.sealPlate": function(params, parsed, data) {
		var model = commandHelper.getParsedValue(parsed, data, 'object', 'model');
		var location0 = commandHelper.getParsedValue(parsed, data, 'object', 'location');

		var destinationAfter = (_.isUndefined(parsed.destinationAfter.objectName)) ? location0 : parsed.destinationAfter.objectName;

		var predicates = [
			{"sealer.canAgentEquipmentProgramModelSite": {
				"agent": parsed.agent.objectName,
				"equipment": parsed.equipment.objectName,
				"program": parsed.program.objectName,
				"model": model,
				"site": parsed.site.objectName
			}}
		];
		var alternatives = commandHelper.queryLogic(data, predicates, '[].and[]."sealer.canAgentEquipmentProgramModelSite"');
		var params2 = alternatives[0];
		//console.log("params2:\n"+JSON.stringify(params2, null, '  '))

		var expansion = [
			(params2.site === location0) ? null : {
				"command": "transporter.movePlate",
				"object": parsed.object.objectName,
				"destination": params2.site
			},
			{
				command: "equipment.run|"+params2.agent+"|"+params2.equipment,
				agent: params2.agent,
				equipment: params2.equipment,
				program: params2.program,
				object: parsed.object.objectName
			},
			(destinationAfter === null) ? null : {
				"command": "transporter.movePlate",
				"object": parsed.object.objectName,
				"destination": location0
			},
		];

		// Create the effects object
		var effects = {};
		effects[parsed.object.objectName + ".sealed"] = true;

		return {
			expansion: expansion,
			effects: effects,
			alternatives: alternatives
		};
	}
};

module.exports = {
	roboliq: "v1",
	objectToPredicateConverters,
	schemas: yaml.load(__dirname+"/../schemas/sealer.yaml"),
	commandHandlers
};
