var _ = require('lodash');
var jmespath = require('jmespath');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');

var commandHandlers = {
	"fluorescenceReader.measurePlate": function(params, data) {
		var parsed = commandHelper.parseParams(params, data, {
			agent: "name?",
			equipment: "name?",
			program: "Object",
			object: "name",
			site: "name?",
			destinationAfter: "name?"
		});
		var model = commandHelper.getParsedValue(parsed, data, 'object', 'model');
		var location0 = commandHelper.getParsedValue(parsed, data, 'object', 'location');

		// TODO: find site using logic (see sealer)

		var destinationAfter = (_.isUndefined(parsed.destinationAfter.valueName)) ? location0 : parsed.destinationAfter.valueName;

		var predicates = [
			{"fluorescenceReader.canAgentEquipmentModelSite": {
				"agent": parsed.agent.valueName,
				"equipment": parsed.equipment.valueName,
				"model": model,
				"site": parsed.site.valueName
			}}
		];
		var alternatives = commandHelper.queryLogic(data, predicates, '[].and[]."fluorescenceReader.canAgentEquipmentModelSite"');
		var params2 = alternatives[0];
		//console.log("params2:\n"+JSON.stringify(params2, null, '  '))

		var expansion = [
			(params2.site === location0) ? null : {
				command: "transporter.movePlate",
				object: parsed.object.valueName,
				destination: params2.site
			},
			{
				command: "fluorescenceReader._run",
				agent: params2.agent,
				equipment: params2.equipment,
				program: parsed.program.value,
				object: parsed.object.valueName
			},
			(destinationAfter === null) ? null : {
				command: "transporter.movePlate",
				object: parsed.object.valueName,
				destination: destinationAfter
			}
		];
		return {
			expansion: expansion
		};
	},
};

module.exports = {
	commandHandlers: commandHandlers
};
