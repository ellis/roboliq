var _ = require('lodash');
var jmespath = require('jmespath');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');

var commandHandlers = {
	"generic.measureFluorescence": function(params, data) {
		var parsed = commandHelper.parseParams(params, data, {
			agent: "name?",
			equipment: "name?",
			object: "Object",
			program: "Object",
			destinationAfter: "name?"
		});
		var location0 = expect.objectsValue({paramName: "object"}, parsed.object.valueName+".location", data.objects);

		// TODO: find site using logic (see sealer)

		var destinationAfter = (_.isUndefined(parsed.destinationAfter.value)) ? location0 : parsed.destinationAfter.value;

		var expansion = [
			(site === location0) ? null : {
				command: "transporter.movePlate",
				object: parsed.object.valueName,
				destination: site
			},
			{
				command: "fluorescenceReader.run",
				agent: agent,
				equipment: equipment,
				program: program
			},
			(!destinationAfter) ? null : {
				command: "transporter.movePlate",
				object: object,
				destination: destinationAfter
			}
		];
		return {
			expansion: expansion
		};
	},
};

module.exports = {
	roboliq: "v1",
	//predicates: predicates,
	objectToPredicateConverters: objectToPredicateConverters,
	commandHandlers: commandHandlers,
	planHandlers: planHandlers
};
