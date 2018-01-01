/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const yaml = require('yamljs');
const commandHelper = require('../commandHelper.js');
const expect = require('../expect.js');
const misc = require('../misc.js');


const commandHandlers = {
	"system._description": function(params, parsed, data) {
		// Do nothing
		return {};
	},
	"system._echo": function(params, parsed, data) {
		// Do nothing
		return {};
	},
	"system.call": function(params, parsed, data) {
		// console.log("system.call: "+JSON.stringify(parsed, null, '\t'))
		switch (parsed.value.name.type) {
			case "Template":
				let expansion = misc.renderTemplate(parsed.value.name.template, parsed.value.lazyParams, data);
				// console.log("system.call: expansion = "+JSON.stringify(expansion, null, '\t'));
				if (_.isString(expansion)) {
					expansion = JSON.parse(expansion);
				}
				return {expansion};
				break;
			default:
				expect.truthy({paramName: "name"}, false, "expected an object of type 'Template'");
				return {};
		}
	},
	"system.description": function(params, parsed, data) {
		//console.log("system.echo: "+JSON.stringify(parsed, null, '\t'))
		return {
			expansion: [_.merge({}, {
				command: "system._description",
				text: parsed.value.value.toString()
			})]
		};
	},
	"system.echo": function(params, parsed, data) {
		//console.log("system.echo: "+JSON.stringify(parsed, null, '\t'))
		return {
			expansion: [_.merge({}, {
				command: "system._echo",
				name: parsed.objectName.value,
				value: parsed.value.value
			})]
		};
	},
	"system.if": function(params, parsed, data) {
		// console.log("system.if:"); console.log({parsed, expansion: (parsed.value.test) ? parsed.value.then : parsed.value.else})
		return {expansion: (parsed.value.test) ? parsed.value.then : parsed.value.else};
	},
	"system.repeat": function(params, parsed, data) {
		const expansion = {};
		if (parsed.value.steps) {
			const count = parsed.value.count;
			for (let i = 1; i <= count; i++) {
				let iteration = _.cloneDeep(parsed.value.steps);
				if (parsed.value.variableName) {
					if (_.isArray(iteration)) {
						iteration = {"1": iteration};
					}
					iteration["@SCOPE"] = {[parsed.value.variableName]: i};
				}
				expansion[i] = iteration;
			}
		}

		return {expansion};
	},
	"system.runtimeExitLoop": function(params, parsed, data) {
		// Do nothing, this command is its own instruction
		return {};
	},
	"system.runtimeLoadVariables": function(params, parsed, data) {
		// Do nothing, this command is its own instruction
		return {};
	},
	"system.runtimeSteps": function(params, parsed, data) {
		// Do nothing, this command is its own instruction
		return {};
	}
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+"/../schemas/system.yaml"),
	commandHandlers
};
