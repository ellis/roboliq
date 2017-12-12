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
	"Scale": function(name, object) {
		return [{ "isScale": { "equipment": name } }];
	},
};

const commandHandlers = {
	"scale.weighLabware": function(params, parsed, data) {
		// console.log(JSON.stringify(parsed));
		var model = commandHelper.getParsedValue(parsed, data, 'object', 'model');
		var location0 = commandHelper.getParsedValue(parsed, data, 'object', 'location');

		var predicates = [
			{"scale.canAgentEquipmentModelSite": {
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment,
				"model": model,
				"site": parsed.objectName.site
			}}
		];
		var [params2, alternatives] = commandHelper.queryLogic(data, predicates, "scale.weighLabware");
		// console.log("params2:\n"+JSON.stringify(params2, null, '  '))
		// console.log("parsed.value.outputFile: "+JSON.stringify(parsed.value.outputFile));

		const destinationAfter
			= (parsed.value.destinationAfter === "stay") ? null
			: _.isUndefined(parsed.objectName.destinationAfter) ? location0
			: parsed.objectName.destinationAfter;

		// Program to pass to sub-command
		const program = mergeR({}, parsed.orig.program);
		// Handle deprecated parameter names
		const output = mergeR({}, parsed.orig.output, {
			joinKey: _.get(parsed.orig, "program.wellDesignFactor"),
			userValues: _.get(parsed.orig, "program.userValues"),
			writeTo: _.get(parsed.orig, "outputFile"),
			appendTo: _.get(parsed.orig, "outputDataset"),
		});
		// console.log({output})

		var expansion = [
			(params2.site === location0) ? null : {
				command: "transporter.movePlate",
				object: parsed.objectName.object,
				destination: params2.site
			},
			_.merge({}, {
				command: `equipment.run|${params2.agent}|${params2.equipment}`,
				agent: params2.agent,
				equipment: params2.equipment,
				measurementType: "weight",
				program: (_.isEmpty(program)) ? undefined : program,
				object: parsed.objectName.object,
				output: (_.isEmpty(output)) ? undefined : output
			}),
			(destinationAfter === null || destinationAfter === params2.site) ? null : {
				command: "transporter.movePlate",
				object: parsed.objectName.object,
				destination: destinationAfter
			}
		];
		// console.log({expansion1: expansion[0]})
		// console.log({expansion1output: expansion[1].output})

		const result = {expansion};

		if (_.has(parsed.value, ["output", "simulated"])) {
			simulatedHelpers.simulatedByLabware(parsed, data, [parsed.objectName.object], result);
		}

		// console.log("RESULTS:\n"+JSON.stringify(result))
		return result;
	},
};

module.exports = {
	roboliq: "v1",
	objectToPredicateConverters,
	schemas: yaml.load(__dirname+"/../schemas/scale.yaml"),
	commandHandlers
};
