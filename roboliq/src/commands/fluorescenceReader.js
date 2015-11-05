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

		var destinationAfter = (_.isUndefined(parsed.destinationAfter.objectName)) ? location0 : parsed.destinationAfter.objectName;

		var predicates = [
			{"fluorescenceReader.canAgentEquipmentModelSite": {
				"agent": parsed.agent.objectName,
				"equipment": parsed.equipment.objectName,
				"model": model,
				"site": parsed.site.objectName
			}}
		];
		var alternatives = commandHelper.queryLogic(data, predicates, '[].and[]."fluorescenceReader.canAgentEquipmentModelSite"');
		var params2 = alternatives[0];
		//console.log("params2:\n"+JSON.stringify(params2, null, '  '))
		//console.log("parsed.outputFile: "+JSON.stringify(parsed.outputFile));

		var expansion = [
			(params2.site === location0) ? null : {
				command: "transporter.movePlate",
				object: parsed.object.objectName,
				destination: params2.site
			},
			_.merge({}, {
				command: ["equipment.run", params2.agent, params2.equipment].join('|'),
				agent: params2.agent,
				equipment: params2.equipment,
				programFile: parsed.programFile.value,
				programData: parsed.programData.value,
				outputFile: parsed.outputFile.value
			}),
			(destinationAfter === null) ? null : {
				command: "transporter.movePlate",
				object: parsed.object.objectName,
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
	commandSpecs: yaml.load(__dirname+"/../commandSpecs/fluorescenceReader.yaml"),
	commandHandlers
};
