/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

/**
 * Helper functions for simulating measurements.
 * @module commands/simulatedHelpers
 */

var _ = require('lodash');
import math from 'mathjs';
var commandHelper = require('../commandHelper.js');
const Design = require('../design.js');
import wellsParser from '../parsers/wellsParser.js';

export function simulatedByWells(parsed, data, wells0, result) {
	// console.log(JSON.stringify(parsed, null, '\t'))
	// console.log({SCOPE: data.objects.SCOPE})
	let simulatedOutput;
	if (_.has(parsed.value, ["output", "simulated"])) {
		const joinKey = _.get(parsed.value, ["output", "joinKey"]);
		const userValues = _.get(parsed.value, ["output", "userValues"], {});

		const wells = _.uniq(_.map(wells0, x => x.replace(/.*\(([^)]*)\)/, "$1")));

		const common = (_.isEmpty(joinKey)) ? data.objects.SCOPE : {};
		// console.log({common})
		// console.log("DATA:\n"+JSON.stringify(data.objects.DATA))
		simulatedOutput = _.map(wells, (well, i) => {
			const row0 = (!_.isUndefined(joinKey))
				? _.find(data.objects.DATA, row => (row[joinKey].replace(/.*\(([^)]*)\)/, "$1") === well)) || {}
				: common;
			const scope = _.defaults({}, row0, data.objects.SCOPE);
			// console.log({i, row0, simulated: parsed.value.output.simulated})
			const value = Design.calculate(parsed.value.output.simulated, scope);
			const row = _.merge({RUNID: "simulated", object: parsed.objectName.object}, row0, userValues, {well, value_type: "absorbance", value});
			// console.log("row: "+JSON.stringify(row))
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

	if (simulatedOutput) {
		if (_.has(parsed.value, ["output", "writeTo"])) {
			_.set(result, ["simulatedOutput", parsed.value.output.writeTo+".json"], simulatedOutput);
		}
		if (_.has(parsed.value, ["output", "appendTo"])) {
			_.set(result, ["simulatedOutput", parsed.value.output.appendTo+".jsonl"], _.get(data, ["simulatedOutput", parsed.value.output.appendTo+".jsonl"], []).concat(simulatedOutput));
		}
	}

	return simulatedOutput;
}

export function simulatedByLabware(parsed, data, labwares0, result) {
	// console.log(JSON.stringify(parsed, null, '\t'))
	// console.log({SCOPE: data.objects.SCOPE})
	let simulatedOutput;
	if (_.has(parsed.value, ["output", "simulated"])) {
		const joinKey = _.get(parsed.value, ["output", "joinKey"]);
		const userValues = _.get(parsed.value, ["output", "userValues"], {});

		const labwares = _.uniq(_.map(labwares0, x => x.replace(/.*\(([^)]*)\)/, "$1")));

		const common = (_.isEmpty(joinKey)) ? data.objects.SCOPE : {};
		// console.log({common})
		// console.log("DATA:\n"+JSON.stringify(data.objects.DATA))
		simulatedOutput = _.map(labwares, labware => {
			const row0 = (!_.isUndefined(joinKey))
				? _.find(data.objects.DATA, row => (row[joinKey].replace(/.*\(([^)]*)\)/, "$1") === labware)) || {}
				: common;
			const scope = _.defaults({}, row0, data.objects.SCOPE);
			// console.log({row0, row1, simulated: parsed.value.output.simulated})
			const value = Design.calculate(parsed.value.output.simulated, scope);
			const row = _.merge({RUNID: "simulated", object: parsed.objectName.object}, row0, userValues, {labware, value_type: "absorbance", value});
			// console.log("row: "+JSON.stringify(row))
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

	if (simulatedOutput) {
		if (_.has(parsed.value, ["output", "writeTo"])) {
			_.set(result, ["simulatedOutput", parsed.value.output.writeTo+".json"], simulatedOutput);
		}
		if (_.has(parsed.value, ["output", "appendTo"])) {
			_.set(result, ["simulatedOutput", parsed.value.output.appendTo+".jsonl"], _.get(data, ["simulatedOutput", parsed.value.output.appendTo+".jsonl"], []).concat(simulatedOutput));
		}
	}

	return simulatedOutput;
}
