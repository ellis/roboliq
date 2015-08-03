var _ = require('lodash');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');

var commandHandlers = {
	"system.repeat": function(params, data) {
		var count = commandHelper.getNumberParameter(params, data, 'count');

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
	commandHandlers: commandHandlers
};
