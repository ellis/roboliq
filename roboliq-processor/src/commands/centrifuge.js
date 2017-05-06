/**
 * Centrifuge commands module (see {@tutorial Commands#centrifuge} or [command specification](tutorial-Commands.html#centrifuge)).
 *
 * THIS SECTION IS FOR TESTING ONLY:
 * {@link loadEvowareCarrierData}
 * {@link module:commandHelper}
 * {@link module:commands/centrifuge}
 * END OF SECTION
 *
 * See {@link roboliq#Protocol}.
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
		return [{ "isCentrifuge": { "equipment": name } }];
	},
};


function centrifuge2(params, parsed, data) {
	var llpl = require('../HTN/llpl.js').create();
	llpl.initializeDatabase(data.predicates);

	//console.log(JSON.stringify(parsed, null, '\t'))

	var agent = parsed.objectName.agent || "?agent";
	var equipment = parsed.objectName.equipment || "?equipment";

	var object1 = parsed.value.object1;
	var object2 = parsed.value.object2;

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

	// Pick a plan
	let chosen = undefined;
	if (data.planAlternativeChoosers.hasOwnProperty("centrifuge.canAgentEquipmentModelSite1Site2")) {
		chosen = data.planAlternativeChoosers["centrifuge.canAgentEquipmentModelSite1Site2"](alternatives, data);
		// console.log({chosen})
	}
	const params2 = chosen || alternatives[0];

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
			command: "equipment.close",
			agent: params2.agent,
			equipment: params2.equipment
		},
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

	const warnings = (object1.model != object2.model)
			? ["object1 and object2 are of different labware models; this may be problematic for centrifugation."]
			: [];

	//console.log("centrifuge2 expansion:")
	//console.log(JSON.stringify(expansion, null, '\t'))
	return {
		expansion,
		alternatives,
		warnings
	};
}

function insertPlates2(params, parsed, data) {
	// console.log("insertPlates2: "+JSON.stringify(parsed, null, '\t'));
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

	const model = (object1) ? object1.model : object2.model;

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

	// Pick a plan
	let chosen = undefined;
	if (data.planAlternativeChoosers.hasOwnProperty("centrifuge.canAgentEquipmentModelSite1Site2")) {
		chosen = data.planAlternativeChoosers["centrifuge.canAgentEquipmentModelSite1Site2"](alternatives, data);
		// console.log({chosen})
	}
	const params2 = chosen || alternatives[0];

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

	const warnings = (parsed.input.object1Model && parsed.input.object2Model && parsed.input.object1Model != parsed.input.object2Model)
			? ["object1 and object2 are of different labware models; this may be problematic for centrifugation."]
			: [];

	return { expansion, alternatives, warnings };
}
insertPlates2.inputSpec = {
	agent: "?agent",
	equipment: "?equipment",
	object1: "?object1",
	object2: "?object2",
	object1Model: "?object1*model",
	object2Model: "?object2*model",
	site1: "?site1",
	site2: "?site2"
};

/**
 * Handlers for {@link centrifuge} commands.
 * @static
 */
var commandHandlers = {
	"centrifuge.centrifuge2": centrifuge2,
	"centrifuge.insertPlates2": insertPlates2
};

module.exports = {
	roboliq: "v1",
	objectToPredicateConverters,
	schemas: yaml.load(__dirname+"/../schemas/centrifuge.yaml"),
	commandHandlers
};
