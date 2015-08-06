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
	"equipment._open": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment"]);
		return {effects: _.zipObject([[params.equipment+".open", true]])};
	},
	"equipment._openSite": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment", "site"]);
		var equipmentData = expect.objectsValue({}, params.equipment, data.objects);
		expect.truthy({paramName: "site"}, equipmentData.sitesInternal.indexOf(params.site) >= 0, "site must be in `"+params.equipment+".sitesInternal`; `"+params.equipment+".sitesInternal` = "+equipmentData.sitesInternal);

		var effects = {};
		// Open equipment
		effects[params.equipment+".open"] = true;
		// Indicate that the given site is open and the other internal sites are closed
		_.forEach(equipmentData.sitesInternal, function(site) { effects[site+".closed"] = (site != params.site); });
		return {effects: effects};
	},
	"equipment._close": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment"]);
		var equipmentData = expect.objectsValue({}, params.equipment, data.objects);
		var effects = {};
		// Close equipment
		effects[params.equipment+".open"] = false;
		// All internal sites are closed
		_.forEach(equipmentData.sitesInternal, function(site) { effects[site+".closed"] = true; });
		return {effects: effects};
	},
};

var planHandlers = {
	"equipment._close": function(params, parentParams, data) {
		return [{
			command: "equipment._close",
			agent: params.agent,
			equipment: params.equipment
		}];
	},
	"equipment._openSite": function(params, parentParams, data) {
		return [{
			command: "equipment._openSite",
			agent: params.agent,
			equipment: params.equipment,
			site: params.site
		}];
	}
};

module.exports = {
	//predicates: predicates,
	objectToPredicateConverters: objectToPredicateConverters,
	commandHandlers: commandHandlers,
	planHandlers: planHandlers
};
