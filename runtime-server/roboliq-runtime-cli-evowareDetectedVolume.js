'use strict';

var child_process = require('child_process');
var fs = require('fs');
var opts = require('commander');
var mkdirp = require('mkdirp');
var moment = require('moment');
var path = require('path');
var _defaults = require('lodash/defaults');
var _drop = require('lodash/drop');
var _forEach = require('lodash/forEach');
var _get = require('lodash/get');
var _isEmpty = require('lodash/isEmpty');
var _isNaN = require('lodash/isNaN');
var _merge = require('lodash/merge');
var _omit = require('lodash/omit');
var _trim = require('lodash/trim');
// var sendPacket = require('./roboliq-runtime-sendPacket.js');

var designHelper = require('./src/designHelper.js');

opts
	.version("1.0")
	.arguments("<script> <step> <syringeLabwareWellVolume...>")
	.parse(process.argv);
// console.log(opts);

const scriptFile = opts.args[0];
const stepId = opts.args[1];

const scriptDir = path.dirname(scriptFile);
const runFile = path.join(scriptDir, path.basename(scriptFile, ".out.json")+".run");
const runId = _trim(fs.readFileSync(runFile));
const runDir = path.join(scriptDir, runId);

const date = moment();
const prefix = date.format("YYYYMMDD_HHmmss")+"-"+stepId+"-"; // TODO: might need to change this to local time/date

// Make sure the run directory exists
mkdirp.sync(runDir);

const protocol = require(scriptFile);
const step = _get(protocol.steps, stepId);
const output = _defaults({}, _get(step, "output"), {joinKey: "well", appendTo: "volume"});
const DATA = _get(protocol.reports, [stepId, "measurementFactors"]);
// console.log(step)

function toNumber(s) {
	const n = Number(s);
	return (_isNaN(n)) ? s : n;
}

const table0 = _drop(opts.args, 2).map(s => {
	const l = s.split(",");
	const row = {
		step_agent: step.agent,
		step_equipment: step.equipment,
		step_program: step.program,
		step_syringe: toNumber(l[0]),
		step_labware: l[1],
		well: l[2],
		value_type: "volume",
		value_units: "ul",
		value: toNumber(l[3])
	};
	return row;
});
// console.log(table0)

// Values that should be in all rows
const common = _defaults({RUNID: runId, date: date.toISOString()}, designHelper.getCommonValues(DATA));

let table;
if (!_isEmpty(DATA)) {
	const joinKey = output.joinKey;
	if (!_isEmpty(joinKey)) {
		const wellToFactors = {};
		_forEach(DATA, factorRow => {
			const well = factorRow[joinKey];
			wellToFactors[well] = factorRow;
		});
		// console.log(wellToFactors);

		// For each row in the measurement table, try to add the factor columns
		table = table0.map(measurementRow0 => {
			const well = measurementRow0.well;
			// Omit the "well" column from the measurement row, unless the joinKey = "well"
			const measurementRow = (joinKey === "well") ? measurementRow0 : _.omit(measurementRow0, "well");
			// console.log(measurementRow)
			// Try to get the factors for this well
			const factorRow = wellToFactors[well];
			return _merge({}, common, factorRow, output.userValues, measurementRow);
		});
	}
	// console.log(table);
}

if (!table) {
	table = table0.map(measurementRow => {
		// console.log(measurementRow)
		return _merge({}, common, output.userValues, measurementRow);
	});
}

// Save the JSON file
if (!_isEmpty(output.writeTo)) {
	const filename = path.join(runDir, `${prefix}${output.writeTo}.json`);
	fs.writeFileSync(filename, JSON.stringify(table, null, '\t'));
}

if (!_isEmpty(output.appendTo)) {
	const filename = path.join(runDir, `${output.appendTo}.jsonl`);
	const contents = table.map(s => JSON.stringify(s)).join("\n") + "\n";
	console.log(contents)
	fs.appendFileSync(filename, contents);
}
