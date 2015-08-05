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
	"centrifuge.instruction.run": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment", "program"]);
		var effects = {};
		closeAll(params, data, effects);
		return {effects: effects};
	},
	"centrifuge.instruction.openSite": function(params, data) {
		expect.paramsRequired(params, ["agent", "equipment", "site"]);
		var equipmentData = expect.objectsValue({}, params.equipment, data.objects);
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
		closeAll(params, data, effects);
		return {effects: effects};
	},
	// TODO:
	// - [ ] raise and error if the sealer site is occupied
	// - [ ] raise error if plate's location isn't set
	// - [ ] return result of query for possible alternative settings
	"centrifuge.centrifuge2": function(params, data) {
		var llpl = require('../HTN/llpl.js').create();
		llpl.initializeDatabase(data.predicates);

		var agent = params.agent || "?agent";
		var equipment = params.equipment || "?equipment";

		var object1 = commandHelper.getObjectParameter(params, data, 'object1');
		var object2 = commandHelper.getObjectParameter(params, data, 'object2');
		if (object1.model != object2.model)
			return {errors: ["object1 and object2 must have the same model for centrifugation."]};

		var query0 = {
			"centrifuge.canAgentEquipmentModelSite1Site2": {
				"agent": "?agent",
				"equipment": "?equipment",
				"model": "?model",
				"site1": "?site1",
				"site2": "?site2"
			}
		};
		var query = _.merge({}, query0,
			{"centrifuge.canAgentEquipmentModelSite1Site2": {
				"agent": params.agent,
				"equipment": params.equipment,
				"model": object1.model,
				"site1": params.site1,
				"site2": params.site2
			}}
		);
		var resultList = llpl.query(query);
		var alternatives = jmespath.search(resultList, '[]."centrifuge.canAgentEquipmentModelSite1Site2"');
		if (_.isEmpty(resultList)) {
			var resultList2 = llpl.query(query0);
			if (_.isEmpty(resultList2)) {
				return {
					errors: ["missing equipment data (please add predicates `centrifuge.canAgentEquipmentModelSite1Site`)"]
				};
			} else {
				return {
					errors: ["missing equipment configuration for " + JSON.stringify(query)]
				};
			}
		}

		// Find any parameters which can only take one specific value
		var params2 = alternatives[0];
		console.log("alternatives[0]:\n"+JSON.stringify(params2))

		var destination1 =
			_.isUndefined(params.destinationAfter1) ? object1.location
			: (params.destinationAfter1 === null) ? params2.site1
			: params.destinationAfter1;
		var destination2 =
			_.isUndefined(params.destinationAfter2) ? object2.location
			: (params.destinationAfter2 === null) ? params2.site2
			: params.destinationAfter2;

		var expansion = {
			"1": {
				"command": "transporter.action.movePlate",
				"object": params.object1,
				"destination": params2.site1
			},
			"2": {
				"command": "transporter.action.movePlate",
				"object": params.object2,
				"destination": params2.site2
			},
			"3": {
				command: "centrifuge.instruction.run",
				agent: params2.agent,
				equipment: params2.equipment,
				program: params.program
			},
			"4": {
				"command": "transporter.action.movePlate",
				"object": params.object1,
				"destination": destination1
			},
			"5": {
				"command": "transporter.action.movePlate",
				"object": params.object2,
				"destination": destination2
			},
		};

		// Create the effets object
		var effects = {};

		return {
			expansion: expansion,
			effects: effects,
			alternatives: alternatives
		};
	}
};

var planHandlers = {
	"centrifuge.instruction.close": function(params, parentParams, data) {
		return [{
			command: "centrifuge.instruction.close",
			agent: params.agent,
			equipment: params.equipment
		}];
	},
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
	//predicates: predicates,
	objectToPredicateConverters: objectToPredicateConverters,
	commandHandlers: commandHandlers,
	planHandlers: planHandlers
};
