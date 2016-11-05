/**
 * Absorbance Reader commands module.
 * @module commands/absorbanceReader
 * @return {Protocol}
 * @version v1
 */

var _ = require('lodash');
var jmespath = require('jmespath');
import math from 'mathjs';
import yaml from 'yamljs';
var commandHelper = require('../commandHelper.js');
const Design = require('../design.js');
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
		if (_.has(parsed.value, ["output", "simulated"])) {
			const labwareModelName = parsed.value.object.model; // REFACTOR: above this is called `model`
			const labwareModel = _.get(data.objects, labwareModelName);
			const joinKey = _.get(parsed.value, ["output", "joinKey"]);
			const userValues = _.get(parsed.value, ["output", "userValues"], {});

			const wells0 = (_.has(parsed.value, ["program", "wells"]))
				? commandHelper.asArray(parsed.value.program.wells)
				: (!_.isUndefined(joinKey))
					? commandHelper.getDesignFactor(joinKey, data.objects.DATA)
					: wellsParser.parse(`${parsed.objectName.object}(all)`, data.objects);
			const wells = _.uniq(_.map(wells0, x => x.replace(/.*\(([^)]*)\)/, "$1")));
			// console.log({wells})

			// console.log({joinKey})
			const common = (_.isEmpty(joinKey)) ? Design.getCommonValues(data.objects.DATA) : {};
			// console.log({common})
			// console.log("DATA:\n"+JSON.stringify(data.objects.DATA))
			simulatedOutput = _.map(wells, well => {
				const row0 = (!_.isUndefined(joinKey))
					? _.find(data.objects.DATA, row => (row[joinKey].replace(/.*\(([^)]*)\)/, "$1") === well)) || {}
					: common;
				const row1 = _.merge({}, data.objects.SCOPE, row0);
				// console.log({row0, row1, simulated: parsed.value.output.simulated})
				const value = Design.calculate(parsed.value.output.simulated, row1);
				const row = _.merge({RUNID: "simulated", object: parsed.objectName.object}, row1, userValues, {well, value_type: "absorbance", value});
				// console.log({row})
				return row;
			});

			if (_.has(parsed.value, ["output", "units"])) {
				_.forEach(simulatedOutput, row => {
					_.forEach(parsed.value.output.units, (units, key) => {
						if (_.has(row, key)) {
							// console.log(row)
							// console.log({key, units, value: row[key]});
							// console.log({a: math.eval(row[key])})
							row[key] = math.eval(row[key]).toNumber(units);
						}
					});
				});
			}
		}

		const result = {expansion};
		if (simulatedOutput) {
			if (_.has(parsed.value, ["output", "writeTo"])) {
				_.set(result, ["simulatedOutput", parsed.value.output.writeTo+".json"], simulatedOutput);
			}
			if (_.has(parsed.value, ["output", "appendTo"])) {
				_.set(result, ["simulatedOutput", parsed.value.output.appendTo+".jsonl"], _.get(data, ["simulatedOutput", parsed.value.output.appendTo+".jsonl"], []).concat(simulatedOutput));
			}
		}
		// console.log("RESULTS:\n"+JSON.stringify(result))
		return result;
	},
};

module.exports = {
	roboliq: "v1",
	schemas: yaml.load(__dirname+"/../schemas/absorbanceReader.yaml"),
	commandHandlers
};
