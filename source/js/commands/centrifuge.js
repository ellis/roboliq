var _ = require('lodash');
var expect = require('../expect.js');
var misc = require('../misc.js');

var predicates = [
	{"action": {"description": "centrifuge.instruction.openSite: open an internal site on the centrifuge",
		"task": {"centrifuge.instruction.openSite": {"agent": "?agent", "equipment": "?equipment", "site": "?site"}},
		"preconditions": [],
		"deletions": [],
		"additions": []
	}},
];

var objectToPredicateConverters = {
	"Sealer": function(name, object) {
		return {
			value: [{
				"isSealer": {
					"equipment": name
				}
			}]
		};
	},
};

function closeAll(params, data, effects) {
	expect.paramsRequired(params, ["equipment"]);
	var equipmentData = misc.getObjectsValue(params.equipment, data.objects);
	// Close equipment
	effects[params.equipment+".open"] = false;
	// Indicate that all internal sites are closed
	_.forEach(equipmentData.sitesInternal, function(site) { effects[site+".closed"] = true; });
}

var commandHandlers = {
	"centrifuge.instruction.run": function(params, data) {
		var effects = {};
		closeAll(params, data, effects);
		return {effects: effects};
	},
	"centrifuge.instruction.openSite": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment", "site"]);
		var equipmentData = misc.getObjectsValue(params.equipment, data.objects);
		expect.truthy({paramName: "site"}, equipmentData.sitesInternal.indexOf(params.site) >= 0, "site must be in `"+params.equipment+".sitesInternal`; `"+params.equipment+".sitesInternal` = "+equipmentData.sitesInternal);

		var effects = {};
		// Close equipment
		effects[params.equipment+".open"] = true;
		// Indicate that all internal sites are closed
		_.forEach(equipmentData.sitesInternal, function(site) { effects[site+".closed"] = (site != params.site); });
		return {effects: effects};
	},
	"centrifuge.instruction.close": function(params, data) {
		var effects = {};
		closeAll(params, data);
		return {effects: effects};
	},
};

var planHandlers = {
	"centrifuge.instruction.openSite": function(params, parentParams, data) {
		return [{
			command: "centrifuge.instruction.openSite",
			agent: params.agent,
			equipment: params.equipment,
			site: params.site
		}];
	}
};

module.exports = {
	predicates: predicates,
	objectToPredicateConverters: objectToPredicateConverters,
	commandHandlers: commandHandlers,
	planHandlers: planHandlers
};
