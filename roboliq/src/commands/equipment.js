var _ = require('lodash');
var jmespath = require('jmespath');
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');

var objectToPredicateConverters = {
	"Centrifuge": function(name, object) {
		return {
			value: [{
				"isCentrifuge": {
					"equipment": name
				}
			}]
		};
	},
};

function closeAll(params, data, effects) {
	expect.paramsRequired(params, ["equipment"]);
	var equipmentData = expect.objectsValue({}, params.equipment, data.objects);
	// Close equipment
	effects[params.equipment+".open"] = false;
	// Indicate that all internal sites are closed
	_.forEach(equipmentData.sitesInternal, function(site) { effects[site+".closed"] = true; });
}

var commandHandlers = {
	"equipment._run": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment"]);
		return {};
	},
	"equipment.open": function(params, data) {
		var parsed = commandHelper.parseParams(params, data, {
			agent: "name",
			equipment: "name"
		});

		var expansion = [{
			command: "equipment.open|"+parsed.agent.valueName+"|"+parsed.equipment.valueName,
			agent: parsed.agent.valueName,
			equipment: parsed.equipment.valueName
		}];

		return {
			expansion: expansion,
			effects: _.zipObject([[params.equipment+".open", true]])
		};
	},
	"equipment.openSite": function(params, data) {
		var parsed = commandHelper.parseParams(params, data, {
			agent: "name",
			equipment: "name",
			site: "name"
		});
		var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");
		expect.truthy({paramName: "site"}, sitesInternal.indexOf(params.site) >= 0, "site must be in `"+params.equipment+".sitesInternal`; `"+params.equipment+".sitesInternal` = "+sitesInternal);

		var expansion = [{
			command: "equipment.openSite|"+parsed.agent.valueName+"|"+parsed.equipment.valueName,
			agent: parsed.agent.valueName,
			equipment: parsed.equipment.valueName,
			site: parsed.site.valueName
		}];

		var effects = {};
		// Open equipment
		effects[parsed.equipment.valueName+".open"] = true;
		// Indicate that the given site is open and the other internal sites are closed
		_.forEach(sitesInternal, function(site) { effects[site+".closed"] = (site != params.site); });

		return {
			expansion: expansion,
			effects: effects
		};
	},
	"equipment.close": function(params, data) {
		var parsed = commandHelper.parseParams(params, data, {
			agent: "name",
			equipment: "name"
		});
		var sitesInternal = commandHelper.getParsedValue(parsed, data, "equipment", "sitesInternal");

		var expansion = [{
			command: "equipment.close|"+parsed.agent.valueName+"|"+parsed.equipment.valueName,
			agent: parsed.agent.valueName,
			equipment: parsed.equipment.valueName
		}];

		var effects = {};
		// Close equipment
		effects[parsed.equipment.valueName+".open"] = false;
		// Indicate that the internal sites are closed
		_.forEach(sitesInternal, function(site) { effects[site+".closed"] = true; });

		return {
			expansion: expansion,
			effects: effects
		};
	},
};

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
	//predicates: predicates,
	objectToPredicateConverters: objectToPredicateConverters,
	commandHandlers: commandHandlers,
	planHandlers: planHandlers
};