/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const assert = require('assert');
const yaml = require('yamljs');
const commandHelper = require('../commandHelper.js');
const expect = require('../expect.js');
const misc = require('../misc.js');


const commandHandlers = {
	"abstract.incubate": function(params, parsed, data) {
		// TODO: read from configuration to automatically figure out which concrete command to use
		assert(parsed.value.methodCommand);
		const expansion = parsed.value.methodCommand;
		return {expansion};
	},
	"abstract.removeCondensationFromSeal": function(params, parsed, data) {
		// TODO: read from configuration to automatically figure out which concrete command to use
		assert(parsed.value.methodCommand);
		const expansion = parsed.value.methodCommand;
		return {expansion};
	},
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+"/../schemas/abstract.yaml"),
	commandHandlers
};
