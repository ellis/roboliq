var _ = require('lodash');
import yaml from 'yamljs';
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');


var commandHandlers = {
	"system.call": function(params, parsed, data) {
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
	"system.repeat": function(params, parsed, data) {
		var expansion = {};
		if (parsed.steps) {
			const count = parsed.count.value;
			for (let i = 1; i <= count; i++) {
				expansion[i] = _.cloneDeep(parsed.steps.value);
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
