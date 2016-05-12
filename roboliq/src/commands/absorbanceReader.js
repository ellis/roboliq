/**
 * Namespace for the ``absorbanceReader`` commands.
 * @namespace absorbanceReader
 * @version v1
 */

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


/**
 * Handlers for {@link absorbanceReader} commands.
 * @static
 */
var commandHandlers = {
	/**
	 * Measure the absorbance of a plate.
	 *
	 * @typedef measurePlate
	 * @memberof absorbanceReader
	 * @property {string} command - "absorbanceReader.measurePlate"
	 * @property {string} [agent] - Agent identifier
	 * @property {string} [equipment] - Equipment identifier
	 * @property {Object} program - Program object for measurement
	 * @property {string} outputFile - Filename for output
	 * @property {string} object - Plate identifier
	 * @property {string} [site] - Site identifier in reader
	 * @property {string} [destinationAfter] - Site to move the plate to after measurement
	 */
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
				program: parsed.value.program,
				programFile: parsed.value.programFile,
				programData: parsed.value.programData,
				object: parsed.objectName.object,
				outputFile: parsed.value.outputFile
			}),
			(destinationAfter === null || destinationAfter === params2.site) ? null : {
				command: "transporter.movePlate",
				object: parsed.objectName.object,
				destination: destinationAfter
			}
		];
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
