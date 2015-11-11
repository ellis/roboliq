/**
 * Namespace for the ``centrifuge`` commands.
 * @namespace centrifuge
 * @version v1
 */

/**
 * Centrifuge commands module.
 * @module commands/centrifuge
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
 * Create predicates for objects of type = "Centrifuge"
 * @static
 */
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

/*
function closeAll(parsed, data, effects) {
	// Close equipment
	effects[parsed.equipment.objectName+".open"] = false;
	// Indicate that all internal sites are closed
	_.forEach(parsed.equipment.value.sitesInternal, function(site) { effects[site+".closed"] = true; });
}
*/

/**
 * Handlers for {@link centrifuge} commands.
 * @static
 */
var commandHandlers = {
	/**
	 * Centrifuge using two plates.
	 *
	 * @typedef centrifuge2
	 * @memberof centrifuge
	 * @property {string} command - "centrifuge.centrifuge2"
	 * @property {string} [agent] - Agent identifier
	 * @property {string} [equipment] - Equipment identifier
	 * @property {string} object1 - Plate identifier
	 * @property {string} object2 - Plate identifier
	 * @property {string} [site1] - Location identifier for the centrifugation site of object1
	 * @property {string} [site2] - Location identifier for the centrifugation site of object2
	 * @property {string} [destinationAfter1] - Location identifier for where object1 should be placed after centrifugation
	 * @property {string} [destinationAfter2] - Location identifier for where object2 should be placed after centrifugation
	 */
	"centrifuge.centrifuge2": function(params, parsed, data) {
		var llpl = require('../HTN/llpl.js').create();
		llpl.initializeDatabase(data.predicates);

		var agent = parsed.agent.objectName || "?agent";
		var equipment = parsed.equipment.objectName || "?equipment";

		var object1 = parsed.object1.value;
		var object2 = parsed.object2.value;
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
				"agent": parsed.agent.objectName,
				"equipment": parsed.equipment.objectName,
				"model": object1.model,
				"site1": parsed.site1.objectName,
				"site2": parsed.site2.objectName
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
		//console.log("alternatives[0]:\n"+JSON.stringify(params2))

		var destination1 =
			_.isUndefined(parsed.destinationAfter1.objectName) ? object1.location
			: (parsed.destinationAfter1.objectName === null) ? params2.site1
			: parsed.destinationAfter1.objectName;
		var destination2 =
			_.isUndefined(parsed.destinationAfter2.objectName) ? object2.location
			: (parsed.destinationAfter2.objectName === null) ? params2.site2
			: parsed.destinationAfter2.objectName;

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
					"object": parsed.object1.objectName,
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
					"object": parsed.object2.objectName,
					"destination": params2.site2
				}
			],
			{
				command: ["equipment.run", params2.agent, params2.equipment].join('|'),
				agent: params2.agent,
				equipment: params2.equipment,
				program: parsed.program.objectName || parsed.program.value
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
					"object": parsed.object1.objectName,
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
					"object": parsed.object2.objectName,
					"destination": destination2
				}
			],
			// Close the centrifuge
			(destination1 === params2.site1 && destination2 === params2.site2) ? null : {
				command: "equipment.close",
				agent: params2.agent,
				equipment: params2.equipment
			},
		];

		return {
			expansion: expansion,
			alternatives: alternatives
		};
	},
	/**
	 * Insert up to two plates into the centrifuge.
	 *
	 * @typedef insertPlates2
	 * @memberof centrifuge
	 * @property {string} command - "centrifuge.insertPlates2"
	 * @property {string} [agent] - Agent identifier
	 * @property {string} [equipment] - Equipment identifier
	 * @property {string} [object1] - Plate identifier
	 * @property {string} [object2] - Plate identifier
	 * @property {string} [site1] - Location identifier for the centrifugation site of object1
	 * @property {string} [site2] - Location identifier for the centrifugation site of object2
	 */
	"centrifuge.insertPlates2": function(params, parsed, data) {
		var llpl = require('../HTN/llpl.js').create();
		llpl.initializeDatabase(data.predicates);

		if (!parsed.object1 && !parsed.object2) {
			// do nothing
			return {};
		}

		var agent = parsed.agent.name || "?agent";
		var equipment = parsed.equipment.name || "?equipment";
		var object1 = parsed.object1.value;
		var object2 = parsed.object2.value;

		if (object1 && object2 && object1.model !== object2.model)
			return {errors: ["object1 and object2 must have the same model for centrifugation."]};
		const model = (object1) ? object1.model : object2.model;
		expect.truthy({}, model, "object1 or object2 must have a `model` value");

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
				"agent": parsed.agent.objectName,
				"equipment": parsed.equipment.objectName,
				"model": model,
				"site1": parsed.site1.objectName,
				"site2": parsed.site2.objectName
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
					"object": parsed.object1.objectName,
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
					"object": parsed.object2.objectName,
					"destination": params2.site2
				}
			],
		];

		return {
			expansion: expansion,
			alternatives: alternatives
		};
	}
};

module.exports = {
	roboliq: "v1",
	objectToPredicateConverters,
	schemas: yaml.load(__dirname+"/../schemas/centrifuge.yaml"),
	commandHandlers
};
