var _ = require('lodash');
import yaml from 'yamljs';
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');


var commandHandlers = {
	"system.call": function(params, parsed, data) {
		switch (parsed.value.name.type) {
			case "Template":
				var expansion = misc.renderTemplate(parsed.value.name.template, params.params, data);
				return {expansion: expansion};
				break;
			default:
				expect.truthy({paramName: "name"}, false, "expected an object of type 'Template'");
				return {};
		}
	},
	"system.repeat": function(params, parsed, data) {
		var expansion = {};
		if (parsed.value.steps) {
			const count = parsed.value.count;
			for (let i = 1; i <= count; i++) {
				expansion[i] = _.cloneDeep(parsed.value.steps);
			}
		}

		return {expansion};
	},
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+"/../schemas/system.yaml"),
	commandHandlers
};
