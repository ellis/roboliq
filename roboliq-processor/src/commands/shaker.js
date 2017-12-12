/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

import _ from 'lodash';
import yaml from 'yamljs';
import commandHelper from '../commandHelper.js';
import expect from '../expect.js';
import misc from '../misc.js';

const objectToPredicateConverters = {
	"Shaker": function(name, object) {
		return [{ "isShaker": { "equipment": name } }];
	},
};

const commandHandlers = {
	// TODO:
	// - [ ] raise and error if the shaker site is occupied
	// - [ ] raise error if plate's location isn't set
	// - [ ] return result of query for possible alternative settings
	"shaker.run": function(params, parsed, data) {
		const predicates = [
			{"shaker.canAgentEquipment": {
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment
			}}
		];
		const [params2, alternatives] = commandHelper.queryLogic(data, predicates, "shaker.canAgentEquipment");
		//console.log("params2:\n"+JSON.stringify(params2, null, '  '))

		const expansion = [
			{
				command: "shaker.run|"+params2.agent+"|"+params2.equipment,
				agent: params2.agent,
				equipment: params2.equipment,
				program: parsed.orig.program
			}
		];

		return {
			expansion,
			alternatives
		};
	},
	// TODO:
	// - [ ] raise and error if the shaker site is occupied
	// - [ ] raise error if plate's location isn't set
	// - [ ] return result of query for possible alternative settings
	"shaker.shakePlate": function(params, parsed, data) {
		const model = commandHelper.getParsedValue(parsed, data, 'object', 'model');
		const location0 = commandHelper.getParsedValue(parsed, data, 'object', 'location');

		const destinationAfter
			= (parsed.value.destinationAfter === "stay") ? null
			: _.isUndefined(parsed.objectName.destinationAfter) ? location0
			: parsed.objectName.destinationAfter;

		const predicates = [
			{"shaker.canAgentEquipmentSite": {
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment,
				"site": parsed.objectName.site
			}}
		];
		const [params2, alternatives] = commandHelper.queryLogic(data, predicates, "shaker.canAgentEquipmentSite");
		//console.log("params2:\n"+JSON.stringify(params2, null, '  '))

		const expansion = [
			(params2.site === location0) ? null : {
				"command": "transporter.movePlate",
				"object": parsed.objectName.object,
				"destination": params2.site
			},
			{
				command: "shaker.run|"+params2.agent+"|"+params2.equipment,
				agent: params2.agent,
				equipment: params2.equipment,
				program: parsed.orig.program,
				object: parsed.objectName.object
			},
			(destinationAfter === null) ? null : {
				"command": "transporter.movePlate",
				"object": parsed.objectName.object,
				"destination": location0
			},
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
	schemas: yaml.load(__dirname+"/../schemas/shaker.yaml"),
	commandHandlers
};
