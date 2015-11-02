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
var commandHelper = require('../commandHelper.js');
var expect = require('../expect.js');
var misc = require('../misc.js');

const commandSpecs = {
	"fluorescenceReader.measurePlate": {
		description: "Measure the fluorescence of a plate.",
		properties: {
			agent: {description: "Agent identifier", type: "string"},
			equipment: {description: "Equipment identifier", type: "string"},
			program: {description: "Program specification for measurement", type: "object"},
			outputFile: {description: "Filename for output", type: "string"},
			object: {description: "Plate identifier", type: "string"},
			site: {description: "Site identifier in reader", type: "string"},
			destinationAfter: {description: "Site to move the plate to after measurement", "type": "string"},
		},
		required: ["program", "outputFile", "object"],
	},
};

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
	"fluorescenceReader.measurePlate": function(params, data) {
		var parsed = commandHelper.parseParams(params, data, {
			agent: "name?",
			equipment: "name?",
			program: "Object",
			outputFile: "name",
			object: "name",
			site: "name?",
			destinationAfter: "name?"
		});
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
			{
				command: ["equipment.run", params2.agent, params2.equipment].join('|'),
				agent: params2.agent,
				equipment: params2.equipment,
				program: parsed.program.value,
				outputFile: parsed.outputFile.objectName
			},
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
	commandSpecs,
	commandHandlers
};
