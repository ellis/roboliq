/**
 * Absorbance Reader commands module.
 * @module commands/absorbanceReader
 * @return {Protocol}
 * @version v1
 */

var _ = require('lodash');
var jmespath = require('jmespath');
import yaml from 'yamljs';
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');
import wellsParser from '../parsers/wellsParser.js';


/**
 * Handlers for {@link absorbanceReader} commands.
 * @static
 */
var commandHandlers = {
	"absorbanceReader.measurePlate": function(params, parsed, data) {
		//console.log(JSON.stringify(parsed));
		var model = commandHelper.getParsedValue(parsed, data, 'object', 'model');
		var location0 = commandHelper.getParsedValue(parsed, data, 'object', 'location');

		var predicates = [
			{"absorbanceReader.canAgentEquipmentModelSite": {
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment,
				"model": model,
				"site": parsed.objectName.site
			}}
		];
		var alternatives = commandHelper.queryLogic(data, predicates, '[].and[]."absorbanceReader.canAgentEquipmentModelSite"');
		var params2 = alternatives[0];
		//console.log("params2:\n"+JSON.stringify(params2, null, '  '))
		//console.log("parsed.value.outputFile: "+JSON.stringify(parsed.value.outputFile));

		const destinationAfter
			= (parsed.value.destinationAfter === "stay") ? null
			: _.isUndefined(parsed.objectName.destinationAfter) ? location0
			: parsed.objectName.destinationAfter;

		// Program to pass to sub-command
		const program = _.merge({}, parsed.orig.program, {
			wells: (parsed.value.program || {}).wells
		});
		// Handle deprecated parameter names
		const output = _.merge({}, parsed.orig.output, {
			joinKey: _.get(parsed.orig, "program.wellDesignFactor"),
			userValues: _.get(parsed.orig, "program.userValues"),
			writeTo: _.get(parsed.orig, "outputFile"),
			appendTo: _.get(parsed.orig, "outputDataset"),
		});

		var expansion = [
			(params2.site === location0) ? null : {
				command: "transporter.movePlate",
				object: parsed.objectName.object,
				destination: params2.site
			},
			_.merge({}, {
				command: ["equipment.run", params2.agent, params2.equipment].join('|'),
				agent: params2.agent,
				equipment: params2.equipment,
				measurementType: "absorbance",
				program: (_.isEmpty(program)) ? undefined : program,
				programFile: parsed.value.programFile,
				programData: parsed.value.programData,
				object: parsed.objectName.object,
				output: (_.isEmpty(output)) ? undefined : output
			}),
			(destinationAfter === null || destinationAfter === params2.site) ? null : {
				command: "transporter.movePlate",
				object: parsed.objectName.object,
				destination: destinationAfter
			}
		];

		let simulatedOutput;
		if (parsed.value.output.simulate) {
			const labwareModelName = parsed.value.object.model; // REFACTOR: above this is called `model`
			const labwareModel = _.get(data.objects, labwareModelName);
			const wells0 = (_.has(parsed.value, ["program", "wells"]))
				? commandHelper.asArray(parsed.value.program.wells)
				: (_.has(parsed.value, ["program", "output", "joinKey"]))
					? commandHelper.getDesignFactor(parsed.value.output.joinKey, data.objects.DATA)
					: wellsParser.parse(`${parsed.objectName.object}(all)`, data.objects);
			const wells = _.map(wells0, x => _.includes(x, "(")))
			CONTINUE
			console.log({wells})
		}

		return {
			expansion: expansion
		};
	},
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+"/../schemas/absorbanceReader.yaml"),
	commandHandlers
};
