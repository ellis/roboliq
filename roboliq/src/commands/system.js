var _ = require('lodash');
import yaml from 'yamljs';
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');


var commandHandlers = {
	"system.call": function(params, data) {
		var parsed = commandHelper.parseParams(params, data, {
			name: "Object"
		});
		switch (parsed.name.value.type) {
			case "Template":
				var expansion = misc.renderTemplate(parsed.name.value.template, params.params, data);
				return {expansion: expansion};
				break;
			default:
				expect.truthy({paramName: "name"}, false, "expected an object of type 'Template'");
				return {};
		}
	},
	"system.repeat": function(params, data) {
		const parsed = commandHelper.parseParams(params, data, {
			count: 'Number'
		});
		var count = parsed.count.value;

		expect.paramsRequired(params, ['steps']);
		var steps = params.steps;

		var expansion = {};
		for (var i = 1; i <= count; i++) {
			expansion[i] = _.cloneDeep(steps);
		}
		return {
			expansion: expansion
		};
	},
};

module.exports = {
	roboliq: "v1",
	commandSpecs: yaml.load(__dirname+"/../commandSpecs/system.yaml"),
	commandHandlers
};
