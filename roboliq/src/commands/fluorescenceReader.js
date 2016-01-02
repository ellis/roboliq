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

		var destinationAfter = (_.isUndefined(parsed.objectName.destinationAfter)) ? location0 : parsed.objectName.destinationAfter;

		var predicates = [
			{"fluorescenceReader.canAgentEquipmentModelSite": {
				"agent": parsed.objectName.agent,
				"equipment": parsed.objectName.equipment,
				"model": model,
				"site": parsed.objectName.site
			}}
		];
		var alternatives = commandHelper.queryLogic(data, predicates, '[].and[]."fluorescenceReader.canAgentEquipmentModelSite"');
		var params2 = alternatives[0];
		//console.log("params2:\n"+JSON.stringify(params2, null, '  '))
		//console.log("parsed.value.outputFile: "+JSON.stringify(parsed.value.outputFile));

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
				programFile: parsed.value.programFile,
				programData: parsed.value.programData,
				outputFile: parsed.value.outputFile
			}),
			(destinationAfter === null) ? null : {
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
	schemas: yaml.load(__dirname+"/../schemas/fluorescenceReader.yaml"),
	commandHandlers
};
