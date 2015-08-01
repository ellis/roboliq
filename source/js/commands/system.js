var _ = require('lodash');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');

var objectToPredicateConverters = {
	"Timer": function(name, object) {
		return {
			value: [{
				"isTimer": {
					"equipment": name
				}
			}]
		};
	},
};

var commandHandlers = {
	"system.repeat": function(params, data) {
		var count = commandHelper.getNumberParameter(params, data, 'count');

		expect.paramsRequired(params, ['body']);
		var body = params.body;

		var expansion = {};
		for (var i = 1; i <= count; i++) {
			expansion[i] = _.cloneDeep(body);
		}
		return {
			expansion: expansion
		};
	},
};

module.exports = {
	objectToPredicateConverters: objectToPredicateConverters,
	commandHandlers: commandHandlers
};
