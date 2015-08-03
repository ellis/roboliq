var _ = require('lodash');
var jmespath = require('jmespath');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');

var objectToPredicateConverters = {
	"Timer": function(name, object) {
		return {
			value: _.compact([
				{"isTimer": {"equipment": name}},
				(object.running) ? {"running": {"equipment": name}} : null
			])
		};
	},
};

var commandHandlers = {
	"timer.instruction.start": function(params, data) {
		var effects = {};
		effects[params.object + ".running"] = true;
		return {
			effects: effects
		};
	},
	"timer.start": function(params, data) {
		var llpl = require('../HTN/llpl.js').create();
		llpl.initializeDatabase(data.predicates);

		var agent = params.agent || "?agent";
		var equipment = params.equipment || "?equipment";

		var query = {"and": [
			{"timer.canAgentEquipment": {
				"agent": agent,
				"equipment": equipment
			}},
			{not: {running: {equipment: equipment}}}
		]};
		var resultList = llpl.query(query);
		//console.log("resultList:\n"+JSON.stringify(resultList))
		var alternatives = jmespath.search(resultList, '[].and[]."timer.canAgentEquipment"');
		if (_.isEmpty(alternatives)) {
			var query2 = {
				"timer.canAgentEquipment": {
					"agent": "?agent",
					"equipment": "?equipment"
				}
			};
			var resultList2 = llpl.query(query2);
			if (_.isEmpty(resultList2)) {
				return {
					errors: ["missing timer data (please add predicates `timer.canAgentEquipment`)"]
				};
			} else {
				return {
					errors: ["missing available timer configuration for " + JSON.stringify(query)]
				};
			}
		}

		var params2 = _.merge(
			{
				command: "timer.instruction.start"
			},
			alternatives[0]
		);

		var expansion = {
			"1": params2
		};

		// Create the effets object
		var effects = {};
		effects[params2.equipment + ".running"] = true;

		return {
			expansion: expansion,
			effects: effects,
			alternatives: alternatives
		};
	},
};

module.exports = {
	objectToPredicateConverters: objectToPredicateConverters,
	commandHandlers: commandHandlers
};
