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

function findAgentEquipmentAlternatives(params, data, running) {
	var llpl = require('../HTN/llpl.js').create();
	llpl.initializeDatabase(data.predicates);

	var agent = params.agent || "?agent";
	var equipment = params.equipment || "?equipment";

	var query1 = {"timer.canAgentEquipment": {
		"agent": agent,
		"equipment": equipment
	}};
	var query2 =
		(running === true) ? {running: {equipment: equipment}}
		: (running === false) ? {not: {running: {equipment: equipment}}}
		: null;
	var query = {"and": _.compact([query1, query2])};

	var resultList = llpl.query(query);
	//console.log("resultList:\n"+JSON.stringify(resultList))
	var alternatives = jmespath.search(resultList, '[].and[]."timer.canAgentEquipment"');
	if (_.isEmpty(alternatives)) {
		var resultList1 = llpl.query(query1);
		if (_.isEmpty(resultList1)) {
			return {
				errors: ["missing timer data (please add predicates `timer.canAgentEquipment`)"]
			};
		} else {
			return {
				errors: ["missing available timer configuration for " + JSON.stringify(query)]
			};
		}
	}

	return alternatives;
}

var commandHandlers = {
	"timer.instruction.start": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment"]);
		var effects = {};
		effects[params.equipment + ".running"] = true;
		return {
			effects: effects
		};
	},
	"timer.instruction.stop": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment"]);
		var effects = {};
		effects[params.equipment + ".running"] = false;
		return {
			effects: effects
		};
	},
	"timer.instruction.wait": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment", "till", "stop"]);
		var effects = {};
		if (params.stop !== false)
			effects[params.equipment + ".running"] = false;
		return {
			effects: effects
		};
	},
	"timer.doAndWait": function(params, data) {
		expect.paramsRequired(params, ['duration', 'steps']);
		var duration = commandHelper.getNumberParameter(params, data, 'duration');
		var steps = params.steps;

		var alternatives = findAgentEquipmentAlternatives(params, data, false);
		if (alternatives.errors) return altenatives;

		var agent = alternatives[0].agent;
		var equipment = alternatives[0].equipment;

		var expansion = {
			1: {
				command: "timer.instruction.start",
				agent: agent,
				equipment: equipment
			},
			2: steps,
			3: {
				command: "timer.instruction.stop",
				agent: agent,
				equipment: equipment
			},
		};

		return {
			expansion: expansion
		};

	},
	"timer.start": function(params, data) {
		var alternatives = findAgentEquipmentAlternatives(params, data, false);
		if (alternatives.errors) return altenatives;

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
		//effects[params2.equipment + ".running"] = true;

		return {
			expansion: expansion,
			effects: effects,
			alternatives: alternatives
		};
	},
	"timer.stop": function(params, data) {
		var alternatives = findAgentEquipmentAlternatives(params, data, true);
		if (alternatives.errors) return alternatives;
		if (alternatives.length > 1) {
			return {errors: ["ambiguous time.stop command, multiple running timers: "+alternatives]};
		}

		var params2 = _.merge(
			{
				command: "timer.instruction.stop"
			},
			alternatives[0]
		);

		var expansion = {
			"1": params2
		};

		// Create the effets object
		var effects = {};
		//effects[params2.equipment + ".running"] = false;

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
