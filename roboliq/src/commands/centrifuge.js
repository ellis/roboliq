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
	effects[parsed.objectName.equipment+".open"] = false;
	// Indicate that all internal sites are closed
	_.forEach(parsed.value.equipment.sitesInternal, function(site) { effects[site+".closed"] = true; });
}
*/

/**
 * Handlers for {@link centrifuge} commands.
 * @static
 */
var commandHandlers = {
	"centrifuge.centrifuge2": function(params, parsed, data) {
		var llpl = require('../HTN/llpl.js').create();
		llpl.initializeDatabase(data.predicates);

		console.log(JSON.stringify(parsed, null, '\t'))

		var agent = parsed.objectName.agent || "?agent";
		var equipment = parsed.objectName.equipment || "?equipment";

		var object1 = parsed.value.object1;
		var object2 = parsed.value.object2;
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
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment,
				"model": object1.model,
				"site1": parsed.objectName.site1,
				"site2": parsed.objectName.site2
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
			_.isUndefined(parsed.objectName.destinationAfter1) ? object1.location
			: (parsed.objectName.destinationAfter1 === null) ? params2.site1
			: parsed.objectName.destinationAfter1;
		var destination2 =
			_.isUndefined(parsed.objectName.destinationAfter2) ? object2.location
			: (parsed.objectName.destinationAfter2 === null) ? params2.site2
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
			// Close the centrifuge
			(destination1 === params2.site1 && destination2 === params2.site2) ? null : {
				command: "equipment.close",
				agent: params2.agent,
				equipment: params2.equipment
			},
		];

		console.log(JSON.stringify(expansion, null, '\t'))
		return {
			expansion: expansion,
			alternatives: alternatives
		};
	},
	"centrifuge.insertPlates2": function(params, parsed, data) {
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
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment,
				"model": model,
				"site1": parsed.objectName.site1,
				"site2": parsed.objectName.site2
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
	}
};

module.exports = {
	roboliq: "v1",
	objectToPredicateConverters,
	schemas: yaml.load(__dirname+"/../schemas/centrifuge.yaml"),
	commandHandlers
};
