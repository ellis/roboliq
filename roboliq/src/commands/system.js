var _ = require('lodash');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');

const commandSpecs = {
	"system.call": {
		description: "Call a template function.\n\nThe template function should be an object of type `Template` with a property `template` holding either a Mustache template string or an object whose properties may be Mustache template strings. The template will be expanded using the values passed in the `params` property.",
		properties: {
			name: {description: "Name of the template function.", type: "string"},
			params: {description: "Parameters to pass to the template function.", type: "object"},
		},
		required: ["name"]
	},
	"system.repeat": {
		description: "Repeat the given command a given number of times.",
		properties: {
			count: {description: "The number of times to repeat.", type: "integer"},
			steps: {description: "The sequence of commands to repeat.", type: "object"}
		},
		required: ["count"]
	}
};

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
	commandSpecs,
	commandHandlers
};
