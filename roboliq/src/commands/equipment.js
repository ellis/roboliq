/**
 * Namespace for the ``equipment`` commands.
 * @namespace equipment
 * @version v1
 */

/**
 * Equipment commands module.
 * @module commands/equipment
 * @return {Protocol}
 * @version v1
 */

var _ = require('lodash');
var jmespath = require('jmespath');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');

function closeAll(params, data, effects) {
	expect.paramsRequired(params, ["equipment"]);
	var equipmentData = expect.objectsValue({}, params.equipment, data.objects);
	// Close equipment
	effects[params.equipment+".open"] = false;
	// Indicate that all internal sites are closed
	_.forEach(equipmentData.sitesInternal, function(site) { effects[site+".closed"] = true; });
}

/**
 * Handlers for {@link equipment} commands.
 * @static
 */
var commandHandlers = {
	/**
	 * Run the given equipment.
	 *
	 * This is a generic command, and any addition parameters may be passed that
	 * are required by the target equipment.
	 *
	 * @typedef _run
	 * @memberof equipment
	 * @property {string} command - "equipment._run"
	 * @property {string} agent - Agent identifier
	 * @property {string} equipment - Equipment identifier
	 */
	"equipment._run": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment"]);
		return {};
	},
	/**
	 * Open the given equipment.
	 *
	 * This is a generic command that expands to a sub-command named
	 * `equipment.open|${agent}|${equipment}`.
	 * That command should be defined in your configuration for your lab.
	 *
	 * The handler should return effects indicating that the equipment is open.
	 *
	 * @typedef open
	 * @memberof equipment
	 * @property {string} command - "equipment.open"
	 * @property {string} agent - Agent identifier
	 * @property {string} equipment - Equipment identifier
	 */
	"equipment.open": function(params, data) {
		var parsed = commandHelper.parseParams(params, data, {
			agent: "name",
			equipment: "name"
		});

		var expansion = [{
			command: "equipment.open|"+parsed.agent.objectName+"|"+parsed.equipment.objectName,
			agent: parsed.agent.objectName,
			equipment: parsed.equipment.objectName
		}];

		return {
			expansion: expansion,
			effects: _.zipObject([[params.equipment+".open", true]])
		};
	},
	/**
	 * Open an equipment site.
	 * This command assumes that only one equipment site can be open at a time.
	 *
	 * This is a generic command that expands to a sub-command named
	 * `equipment.openSite|${agent}|${equipment}`.
	 * That command should be defined in your configuration for your lab.
	 *
	 * The handler should return effects indicating that the equipment is open,
	 * the given site is open, and all other equipment sites are closed.
	 *
	 * @typedef openSite
	 * @memberof equipment
	 * @property {string} command - "equipment.openSite"
	 * @property {string} agent - Agent identifier
	 * @property {string} equipment - Equipment identifier
	 * @property {string} site - Site identifier
	 */
	"equipment.openSite": function(params, data) {
		var parsed = commandHelper.parseParams(params, data, {
			agent: "name",
			equipment: "name",
			site: "name"
		});
		var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");
		expect.truthy({paramName: "site"}, sitesInternal.indexOf(params.site) >= 0, "site must be in `"+params.equipment+".sitesInternal`; `"+params.equipment+".sitesInternal` = "+sitesInternal);

		var expansion = [{
			command: "equipment.openSite|"+parsed.agent.objectName+"|"+parsed.equipment.objectName,
			agent: parsed.agent.objectName,
			equipment: parsed.equipment.objectName,
			site: parsed.site.objectName
		}];

		var effects = {};
		// Open equipment
		effects[parsed.equipment.objectName+".open"] = true;
		// Indicate that the given site is open and the other internal sites are closed
		_.forEach(sitesInternal, function(site) { effects[site+".closed"] = (site != params.site); });

		return {
			expansion: expansion,
			effects: effects
		};
	},
	/**
	 * Close the given equipment.
	 *
	 * This is a generic command that expands to a sub-command named
	 * `equipment.close|${agent}|${equipment}`.
	 * That command should be defined in your configuration for your lab.
	 *
	 * The handler should return effects indicating the the equipment is closed
	 * and all of its sites are closed.
	 *
	 * @typedef close
	 * @memberof equipment
	 * @property {string} command - "equipment.close"
	 * @property {string} agent - Agent identifier
	 * @property {string} equipment - Equipment identifier
	 */
	"equipment.close": function(params, data) {
		var parsed = commandHelper.parseParams(params, data, {
			agent: "name",
			equipment: "name"
		});
		var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");

		var expansion = [{
			command: "equipment.close|"+parsed.agent.objectName+"|"+parsed.equipment.objectName,
			agent: parsed.agent.objectName,
			equipment: parsed.equipment.objectName
		}];

		var effects = {};
		// Close equipment
		effects[parsed.equipment.objectName+".open"] = false;
		// Indicate that the internal sites are closed
		_.forEach(sitesInternal, function(site) { effects[site+".closed"] = true; });

		return {
			expansion: expansion,
			effects: effects
		};
	},
};

/**
 * Plan handler to allow other modules to use `equipment._close` and
 * `equipment._openSite` as planning actions.
 * @static
 */
var planHandlers = {
	"equipment._close": function(params, parentParams, data) {
		return [{
			command: "equipment.close",
			agent: params.agent,
			equipment: params.equipment
		}];
	},
	"equipment._openSite": function(params, parentParams, data) {
		return [{
			command: "equipment.openSite",
			agent: params.agent,
			equipment: params.equipment,
			site: params.site
		}];
	}
};

module.exports = {
	roboliq: "v1",
	commandHandlers: commandHandlers,
	planHandlers: planHandlers
};
