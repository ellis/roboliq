/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Namespace for the ``timer`` commands.
 * @namespace timer
 * @version v1
 */

/**
 * Timer commands module.
 * @module commands/timer
 * @return {Protocol}
 */

var _ = require('lodash');
var jmespath = require('jmespath');
var yaml = require('yamljs');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');

/**
 * Create predicates for objects of type = "Timer"
 * @static
 */
var objectToPredicateConverters = {
	"Timer": function(name, object) {
		return _.compact([
			{"isTimer": {"equipment": name}},
			(object.running) ? {"running": {"equipment": name}} : null
		]);
	},
};

function findAgentEquipmentAlternatives(params, data, running) {
	var llpl = require('../HTN/llpl.js').create();
	//console.log("predicates:\n"+JSON.stringify(data.predicates))
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

	//console.log("query:\n"+JSON.stringify(query, null, '\t'))
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

/**
 * Handlers for {@link timer} commands.
 * @static
 */
var commandHandlers = {
	/**
	 * Sleep for a given duration using a specific timer.
	 *
	 * Handler should return `effects` that the timer is not running.
	 *
	 * @typedef _sleep
	 * @memberof timer
	 * @property {string} command - "timer._sleep"
	 * @property {string} agent - Agent identifier
	 * @property {string} equipment - Equipment identifier
	 * @property {number} duration - Number of seconds to sleep
	 */
	"timer._sleep": function(params, parsed, data) {
		var effects = {};
		if (parsed.value.stop)
			effects[parsed.objectName.equipment + ".running"] = false;
		return {
			effects: effects
		};
	},
	/**
	 * Start the given timer.
	 *
	 * Handler should return `effects` that the timer is running.
	 *
	 * @typedef _start
	 * @memberof timer
	 * @property {string} command - "timer._start"
	 * @property {string} agent - Agent identifier
	 * @property {string} equipment - Equipment identifier
	 */
	"timer._start": function(params, parsed, data) {
		var effects = {};
		effects[parsed.objectName.equipment + ".running"] = true;
		return {
			effects: effects
		};
	},
	/**
	 * Stop the given timer.
	 *
	 * Handler should return `effects` that the timer is not running.
	 *
	 * @typedef _stop
	 * @memberof timer
	 * @property {string} command - "timer._stop"
	 * @property {string} agent - Agent identifier
	 * @property {string} equipment - Equipment identifier
	 */
	"timer._stop": function(params, parsed, data) {
		var effects = {};
		effects[parsed.objectName.equipment + ".running"] = false;
		return {
			effects: effects
		};
	},
	/**
	 * Wait until the given timer has reacher the given elapsed time.
	 *
	 * Handler should:
	 * - expect that the timer (identified by the `equipment` parameter) is running
	 * - return `effects` that the timer is not running
	 *
	 * @typedef _wait
	 * @memberof timer
	 * @property {string} command - "timer._wait"
	 * @property {string} agent - Agent identifier
	 * @property {string} equipment - Equipment identifier
	 * @property {number} till - Number of seconds to wait till from the time the timer was started
	 * @property {boolean} stop - Whether to stop the timer after waiting, or let it continue
	 */
	"timer._wait": function(params, parsed, data) {
		// TODO: assert that timer is running
		var effects = {};
		if (parsed.value.stop)
			effects[parsed.objectName.equipment + ".running"] = false;
		return {
			effects: effects
		};
	},
	/**
	 * A control construct to perform the given sub-steps and then wait
	 * until a certain amount of time has elapsed since the beginning of this command.
	 *
	 * @typedef doAndWait
	 * @memberof timer
	 * @property {string} command - "timer.doAndWait"
	 * @property {string} [agent] - Agent identifier
	 * @property {string} [equipment] - Equipment identifier
	 * @property {number} duration - Number of seconds this command should last
	 * @property {Array|Object} steps - Sub-steps to perform
	 */
	"timer.doAndWait": function(params, parsed, data) {
		var alternatives = findAgentEquipmentAlternatives(params, data, false);
		if (alternatives.errors) return alternatives;

		var agent = alternatives[0].agent;
		var equipment = alternatives[0].equipment;

		var expansion = {
			1: {
				command: "timer._start",
				agent: agent,
				equipment: equipment
			},
			2: parsed.value.steps,
			3: {
				command: "timer._wait",
				agent: agent,
				equipment: equipment,
				till: parsed.value.duration.toNumber('s'),
				stop: true
			},
		};

		return {
			expansion: expansion
		};

	},
	/**
	 * Sleep for a given duration.
	 *
	 * @typedef sleep
	 * @memberof timer
	 * @property {string} command - "timer.sleep"
	 * @property {string} [agent] - Agent identifier
	 * @property {string} [equipment] - Equipment identifier
	 * @property {number} duration - Number of seconds to sleep
	 */
	"timer.sleep": function(params, parsed, data) {
		var alternatives = findAgentEquipmentAlternatives(params, data, false);
		if (alternatives.errors) return alternatives;

		var params2 = _.merge(
			{
				command: "timer._sleep",
				duration: parsed.value.duration.format({precision: 6})
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
	/**
	 * Start a timer.
	 *
	 * @typedef start
	 * @memberof timer
	 * @property {string} command - "timer.start"
	 * @property {string} [agent] - Agent identifier
	 * @property {string} [equipment] - Equipment identifier
	 */
	"timer.start": function(params, parsed, data) {
		var alternatives = findAgentEquipmentAlternatives(params, data, false);
		//console.log({alternatives})
		if (alternatives.errors) return alternatives;

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
	/**
	 * Stop a running timer.
	 *
	 * @typedef stop
	 * @memberof timer
	 * @property {string} command - "timer.stop"
	 * @property {string} [agent] - Agent identifier
	 * @property {string} [equipment] - Equipment identifier
	 */
	"timer.stop": function(params, parsed, data) {
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
	"timer.wait": function(params, parsed, data) {
		var alternatives = findAgentEquipmentAlternatives(params, data, true);
		if (alternatives.errors) return alternatives;
		if (alternatives.length > 1) {
			return {errors: ["ambiguous time.wait command, multiple running timers: "+alternatives]};
		}

		var params2 = _.merge(
			{
				command: "timer._wait",
				till: parsed.value.till.format({precision: 14}),
				stop: parsed.value.stop
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
	roboliq: "v1",
	objectToPredicateConverters,
	schemas: yaml.load(__dirname+'/../schemas/timer.yaml'),
	commandHandlers
};
