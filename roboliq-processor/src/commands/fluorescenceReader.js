/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Namespace for the ``fluorescenceReader`` commands.
 * @namespace fluorescenceReader
 * @version v1
 */

/**
 * Fluorescence Reader commands module.
 * @module commands/fluorescenceReader
 * @return {Protocol}
 * @version v1
 */

var _ = require('lodash');
var jmespath = require('jmespath');
import yaml from 'yamljs';
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
import {mergeR} from '../mergeR.js';
var misc = require('../misc.js');


/**
 * Handlers for {@link fluorescenceReader} commands.
 * @static
 */
var commandHandlers = {
	/**
	 * Measure the fluorescence of a plate.
	 *
	 * @typedef measurePlate
	 * @memberof fluorescenceReader
	 * @property {string} command - "fluorescenceReader.measurePlate"
	 * @property {string} [agent] - Agent identifier
	 * @property {string} [equipment] - Equipment identifier
	 * @property {Object} program - Program object for measurement
	 * @property {string} outputFile - Filename for output
	 * @property {string} object - Plate identifier
	 * @property {string} [site] - Site identifier in reader
	 * @property {string} [destinationAfter] - Site to move the plate to after measurement
	 */
	"fluorescenceReader.measurePlate": function(params, parsed, data) {
		//console.log(JSON.stringify(parsed));
		var model = commandHelper.getParsedValue(parsed, data, 'object', 'model');
		var location0 = commandHelper.getParsedValue(parsed, data, 'object', 'location');

		var predicates = [
			{"fluorescenceReader.canAgentEquipmentModelSite": {
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment,
				"model": model,
				"site": parsed.objectName.site
			}}
		];
		var [params2, alternatives] = commandHelper.queryLogic(data, predicates, "fluorescenceReader.canAgentEquipmentModelSite");
		//console.log("params2:\n"+JSON.stringify(params2, null, '  '))
		//console.log("parsed.value.outputFile: "+JSON.stringify(parsed.value.outputFile));

		const destinationAfter
			= (parsed.value.destinationAfter === "stay") ? null
			: _.isUndefined(parsed.objectName.destinationAfter) ? location0
			: parsed.objectName.destinationAfter;

		// Program to pass to sub-command
		const program = mergeR({}, parsed.orig.program, {
			wells: (parsed.value.program || {}).wells
		});
		// console.log({program})
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
				command: ["equipment.run", params2.agent, params2.equipment].join('|'),
				agent: params2.agent,
				equipment: params2.equipment,
				measurementType: "fluorescence",
				program: (_.isEmpty(program)) ? undefined : program,
				programFileTemplate: parsed.value.programFileTemplate,
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
		// console.log({expansion1output: expansion[1].output})

		const result = {expansion};

		if (_.has(parsed.value, ["output", "simulated"])) {
			// Wells are chosen as follows:
			// 1) program.wells
			// 2) output.joinKey
			// 3) all wells on labware
			const wells = (_.has(parsed.value, ["program", "wells"]))
				? commandHelper.asArray(parsed.value.program.wells)
				: (!_.isUndefined(output.joinKey))
					? commandHelper.getDesignFactor(output.joinKey, data.objects.DATA)
					: wellsParser.parse(`${parsed.objectName.object}(all)`, data.objects);
			// console.log({wells})
			simulatedHelpers.simulatedByWells(parsed, data, wells, result);
		}

		// console.log("RESULTS:\n"+JSON.stringify(result))
		return result;
	},
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+"/../schemas/fluorescenceReader.yaml"),
	commandHandlers
};
