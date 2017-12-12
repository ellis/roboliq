/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

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
import yaml from 'yamljs';
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
	"equipment._run": function(params, parsed, data) {
		return {};
	},
	"equipment.close": function(params, parsed, data) {
		var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");

		var expansion = [{
			command: "equipment.close|"+parsed.objectName.agent+"|"+parsed.objectName.equipment,
			agent: parsed.objectName.agent,
			equipment: parsed.objectName.equipment
		}];

		var effects = {};
		// Close equipment
		effects[parsed.objectName.equipment+".open"] = false;
		// Indicate that the internal sites are closed
		_.forEach(sitesInternal, function(site) { effects[site+".closed"] = true; });

		return {
			expansion: expansion,
			effects: effects
		};
	},
	"equipment.open": function(params, parsed, data) {
		var expansion = [{
			command: "equipment.open|"+parsed.objectName.agent+"|"+parsed.objectName.equipment,
			agent: parsed.objectName.agent,
			equipment: parsed.objectName.equipment
		}];

		return {
			expansion: expansion,
			effects: {[parsed.objectName.equipment+".open"]: true}
		};
	},
	"equipment.openSite": function(params, parsed, data) {
		// console.log("equipment.openSite:"); console.log(JSON.stringify(parsed, null, '\t'))
		var sitesInternal = parsed.value.equipment.sitesInternal;
		expect.truthy({paramName: "site"}, sitesInternal.indexOf(parsed.objectName.site) >= 0, `site ${parsed.objectName.site} must be in \`${parsed.objectName.equipment}.sitesInternal\; \`${parsed.objectName.equipment}.sitesInternal\` = ${sitesInternal}`);

		var expansion = [{
			command: "equipment.openSite|"+parsed.objectName.agent+"|"+parsed.objectName.equipment,
			agent: parsed.objectName.agent,
			equipment: parsed.objectName.equipment,
			site: parsed.objectName.site
		}];
		//console.log(JSON.stringify(expansion, null, '\t'))

		var effects = {};
		// Open equipment
		effects[parsed.objectName.equipment+".open"] = true;
		// Indicate that the given site is open and the other internal sites are closed
		_.forEach(sitesInternal, function(site) { effects[site+".closed"] = (site != parsed.objectName.site); });

		//console.log(JSON.stringify(effects, null, '\t'))
		return {
			expansion: expansion,
			effects: effects
		};
	},
	"equipment.start": function(params, parsed, data) {
		var expansion = [_.defaults({
			command: "equipment.start|"+parsed.objectName.agent+"|"+parsed.objectName.equipment,
			agent: parsed.objectName.agent,
			equipment: parsed.objectName.equipment
		}, parsed.orig)];

		var effects = { [parsed.objectName.equipment+".running"]: true };

		return { expansion, effects };
	},
	"equipment.stop": function(params, parsed, data) {
		var expansion = [_.defaults({
			command: "equipment.stop|"+parsed.objectName.agent+"|"+parsed.objectName.equipment,
			agent: parsed.objectName.agent,
			equipment: parsed.objectName.equipment
		}, parsed.orig)];

		var effects = { [parsed.objectName.equipment+".running"]: false };

		return { expansion, effects };
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
	schemas: yaml.load(__dirname+"/../schemas/equipment.yaml"),
	commandHandlers,
	planHandlers
};
