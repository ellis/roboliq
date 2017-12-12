/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Incubator commands module (see {@tutorial Commands#incubator} or [command specification](tutorial-Commands.html#incubator)).
 *
 * See {@link roboliq#Protocol}.
 * @module commands/incubator
 * @return {Protocol}
 * @version v1
 */

var _ = require('lodash');
var jmespath = require('jmespath');
import yaml from 'yamljs';
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');

/**
 * Create predicates for objects of type = "Incubator"
 * @static
 */
var objectToPredicateConverters = {
	"Incubator": function(name, object) {
		return [{ "isIncubator": { "equipment": name } }];
	},
};

/*
function closeAll(parsed, data, effects) {
	// Close equipment
	effects[parsed.objectName.equipment+".open"] = false;
	// Indicate that all internal sites are closed
	_.forEach(parsed.value.equipment.sitesInternal, function(site) { effects[site+".closed"] = true; });
}
*/

/**
 * Handlers for {@link incubator} commands.
 * @static
 */
var commandHandlers = {
	"incubator.incubatePlates": function(params, parsed, data) {
		var llpl = require('../HTN/llpl.js').create();
		llpl.initializeDatabase(data.predicates);

		//console.log(JSON.stringify(parsed, null, '\t'))

		var agent = parsed.objectName.agent || "?agent";
		var equipment = parsed.objectName.equipment || "?equipment";

		var object1 = parsed.value.object1;
		var object2 = parsed.value.object2;
		if (object1.model != object2.model)
			return {errors: ["object1 and object2 must have the same model for centrifugation."]};

		var query0 = {
			"incubator.canAgentEquipmentModelSite1Site2": {
				"agent": "?agent",
				"equipment": "?equipment",
				"model": "?model",
				"site1": "?site1",
				"site2": "?site2"
			}
		};
		var query = _.merge({}, query0,
			{"incubator.canAgentEquipmentModelSite1Site2": {
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment,
				"model": object1.model,
				"site1": parsed.objectName.site1,
				"site2": parsed.objectName.site2
			}}
		);
		var resultList = llpl.query(query);
		var alternatives = jmespath.search(resultList, '[]."incubator.canAgentEquipmentModelSite1Site2"');
		if (_.isEmpty(resultList)) {
			var resultList2 = llpl.query(query0);
			if (_.isEmpty(resultList2)) {
				return {
					errors: ["missing equipment data (please add predicates `incubator.canAgentEquipmentModelSite1Site`)"]
				};
			} else {
				return {
					errors: ["missing equipment configuration for " + JSON.stringify(query)]
				};
			}
		}

		// Find any parameters which can only take one specific value
		var params2 = alternatives[0];
		//console.log("alternatives[0]:\n"+JSON.stringify(params2))

		const destination1
			= (parsed.value.destinationAfter1 === "stay") ? params2.site1
			: _.isUndefined(parsed.objectName.destinationAfter1) ? object1.location
			: parsed.objectName.destinationAfter1;
		const destination2
			= (parsed.value.destinationAfter2 === "stay") ? params2.site2
			: _.isUndefined(parsed.objectName.destinationAfter2) ? object2.location
			: parsed.objectName.destinationAfter2;

		var expansion = [
			(object1.location === params2.site1) ? null : [
				{
					command: "equipment.openSite",
					agent: params2.agent,
					equipment: params2.equipment,
					site: params2.site1
				},
				{
					"command": "transporter.movePlate",
					"object": parsed.objectName.object1,
					"destination": params2.site1
				}
			],
			(object2.location === params2.site2) ? null : [
				{
					command: "equipment.openSite",
					agent: params2.agent,
					equipment: params2.equipment,
					site: params2.site2
				},
				{
					"command": "transporter.movePlate",
					"object": parsed.objectName.object2,
					"destination": params2.site2
				}
			],
			{
				command: ["equipment.run", params2.agent, params2.equipment].join('|'),
				agent: params2.agent,
				equipment: params2.equipment,
				program: parsed.objectName.program || parsed.value.program
			},
			// Move object1 back
			(destination1 === params2.site1) ? null : [
				{
					command: "equipment.openSite",
					agent: params2.agent,
					equipment: params2.equipment,
					site: params2.site1
				},
				{
					"command": "transporter.movePlate",
					"object": parsed.objectName.object1,
					"destination": destination1
				}
			],
			// Move object2 back
			(destination2 === params2.site2) ? null : [
				{
					command: "equipment.openSite",
					agent: params2.agent,
					equipment: params2.equipment,
					site: params2.site2
				},
				{
					"command": "transporter.movePlate",
					"object": parsed.objectName.object2,
					"destination": destination2
				}
			],
			// Close the incubator
			(destination1 === params2.site1 && destination2 === params2.site2) ? null : {
				command: "equipment.close",
				agent: params2.agent,
				equipment: params2.equipment
			},
		];

		//console.log("incubator2 expansion:")
		//console.log(JSON.stringify(expansion, null, '\t'))
		return {
			expansion: expansion,
			alternatives: alternatives
		};
	},
	"incubator.insertPlates": function(params, parsed, data) {
		var llpl = require('../HTN/llpl.js').create();
		llpl.initializeDatabase(data.predicates);

		if (!parsed.value.object1 && !parsed.value.object2) {
			// do nothing
			return {};
		}

		var agent = parsed.objectName.agent || "?agent";
		var equipment = parsed.objectName.equipment || "?equipment";
		var object1 = parsed.value.object1;
		var object2 = parsed.value.object2;

		if (object1 && object2 && object1.model !== object2.model)
			return {errors: ["object1 and object2 must have the same model for centrifugation."]};
		const model = (object1) ? object1.model : object2.model;
		expect.truthy({}, model, "object1 or object2 must have a `model` value");

		var query0 = {
			"incubator.canAgentEquipmentModelSite1Site2": {
				"agent": "?agent",
				"equipment": "?equipment",
				"model": "?model",
				"site1": "?site1",
				"site2": "?site2"
			}
		};
		var query = _.merge({}, query0,
			{"incubator.canAgentEquipmentModelSite1Site2": {
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment,
				"model": model,
				"site1": parsed.objectName.site1,
				"site2": parsed.objectName.site2
			}}
		);
		var resultList = llpl.query(query);
		var alternatives = jmespath.search(resultList, '[]."incubator.canAgentEquipmentModelSite1Site2"');
		if (_.isEmpty(resultList)) {
			var resultList2 = llpl.query(query0);
			if (_.isEmpty(resultList2)) {
				return {
					errors: ["missing equipment data (please add predicates `incubator.canAgentEquipmentModelSite1Site`)"]
				};
			} else {
				return {
					errors: ["missing equipment configuration for " + JSON.stringify(query)]
				};
			}
		}

		// Find any parameters which can only take one specific value
		var params2 = alternatives[0];
		//console.log("alternatives[0]:\n"+JSON.stringify(params2))

		var expansion = [
			(!object1 || object1.location === params2.site1) ? null : [
				{
					command: "equipment.openSite",
					agent: params2.agent,
					equipment: params2.equipment,
					site: params2.site1
				},
				{
					"command": "transporter.movePlate",
					"object": parsed.objectName.object1,
					"destination": params2.site1
				}
			],
			(!object2 || object2.location === params2.site2) ? null : [
				{
					command: "equipment.openSite",
					agent: params2.agent,
					equipment: params2.equipment,
					site: params2.site2
				},
				{
					"command": "transporter.movePlate",
					"object": parsed.objectName.object2,
					"destination": params2.site2
				}
			],
		];

		return {
			expansion: expansion,
			alternatives: alternatives
		};
	},
	"incubator.run": function(params, parsed, data) {
		var llpl = require('../HTN/llpl.js').create();
		llpl.initializeDatabase(data.predicates);

		//console.log(JSON.stringify(parsed, null, '\t'))

		var agent = parsed.objectName.agent || "?agent";
		var equipment = parsed.objectName.equipment || "?equipment";

		var object1 = parsed.value.object1;
		var object2 = parsed.value.object2;
		if (object1.model != object2.model)
			return {errors: ["object1 and object2 must have the same model for centrifugation."]};

		var query0 = {
			"incubator.canAgentEquipment": {
				"agent": "?agent",
				"equipment": "?equipment"
			}
		};
		var query = _.merge({}, query0,
			{"incubator.canAgentEquipment": {
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment
			}}
		);
		var resultList = llpl.query(query);
		var alternatives = jmespath.search(resultList, '[]."incubator.canAgentEquipment"');
		if (_.isEmpty(resultList)) {
			var resultList2 = llpl.query(query0);
			if (_.isEmpty(resultList2)) {
				return {
					errors: ["missing equipment data (please add predicates `incubator.canAgentEquipment`)"]
				};
			} else {
				return {
					errors: ["missing equipment configuration for " + JSON.stringify(query)]
				};
			}
		}

		// Find any parameters which can only take one specific value
		var params2 = alternatives[0];
		//console.log("alternatives[0]:\n"+JSON.stringify(params2))

		var expansion = [
			{
				command: ["equipment.run", params2.agent, params2.equipment].join('|'),
				agent: params2.agent,
				equipment: params2.equipment,
				program: parsed.objectName.program || parsed.value.program
			},
		];

		//console.log("incubator2 expansion:")
		//console.log(JSON.stringify(expansion, null, '\t'))
		return {
			expansion: expansion,
			alternatives: alternatives
		};
	},
};

module.exports = {
	roboliq: "v1",
	objectToPredicateConverters,
	schemas: yaml.load(__dirname+"/../schemas/incubator.yaml"),
	commandHandlers
};
