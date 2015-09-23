
/**
 * Namespace for the ``timer`` commands.
 * @namespace timer
 * @version v1
 */
var timer = {};

/**
 * Timer commands module.
 * @module commands/timer
 * @return {Protocol}
 */

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
	/**
	 * Parameters for timer._sleep
	 *
	 * effects that the timer isn't running
	 *
	 * @typedef _sleep
	 * @memberOf timer
	 * @alias timer._sleep
	 * @property {string} command - "timer._sleep"
	 * @property {string} agent - Agent identifier
	 * @property {string} equipment - Equipment identifier
	 * @property {number} duration - Number of seconds to sleep
	 */
	/**
	 * Instruction to sleep.
	 * @param {string} params.agent Agent identifier
	 * @param {string} params.equipment Equipment identifier
	 * @param {number} params.duration Number of seconds to sleep
	 * @return {CommandHandlerResult} effects that the timer isn't running
	 */
	"timer._sleep": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment", "duration"]);
		var effects = {};
		if (params.stop)
			effects[params.equipment + ".running"] = false;
		return {
			effects: effects
		};
	},
	/**
	 * Instruction to start a timer.
	 * @name _start
	 * @function
	 * @param {string} params.agent Agent identifier
	 * @param {string} params.equipment Equipment identifier
	 * @return {object} effects that the timer is running
	 */
	"timer._start": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment"]);
		var effects = {};
		effects[params.equipment + ".running"] = true;
		return {
			effects: effects
		};
	},
	/**
	 * Instruction to stop a timer.
	 * @name _stop
	 * @function
	 * @param {string} params.agent Agent identifier
	 * @param {string} params.equipment Equipment identifier
	 * @return {object} effects that the timer is not running
	 */
	"timer._stop": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment"]);
		var effects = {};
		effects[params.equipment + ".running"] = false;
		return {
			effects: effects
		};
	},
	"timer._wait": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment", "till", "stop"]);
		var effects = {};
		if (params.stop)
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
				command: "timer._start",
				agent: agent,
				equipment: equipment
			},
			2: steps,
			3: {
				command: "timer._wait",
				agent: agent,
				equipment: equipment,
				till: duration,
				stop: true
			},
		};

		return {
			expansion: expansion
		};

	},
	"timer.sleep": function(params, data) {
		expect.paramsRequired(params, ["duration"]);
		//var duration = misc.getNumberParameter(params, data, "duration");

		var alternatives = findAgentEquipmentAlternatives(params, data, false);
		if (alternatives.errors) return altenatives;

		var params2 = _.merge(
			{
				command: "timer._sleep",
				duration: params.duration
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
	"timer.start": function(params, data) {
		var alternatives = findAgentEquipmentAlternatives(params, data, false);
		if (alternatives.errors) return altenatives;

		var params2 = _.merge(
			{
				command: "timer._start"
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
				command: "timer._stop"
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

/**
 * @type {Protocol}
 */
module.exports = {
	/** Targeted version of Roboliq. */
	roboliq: "v1",
	/** Create predicates for objects of type = "Timer". */
	objectToPredicateConverters: objectToPredicateConverters,
	commandHandlers: commandHandlers
};
