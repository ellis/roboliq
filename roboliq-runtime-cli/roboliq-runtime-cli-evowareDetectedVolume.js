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
	const value0 = toNumber(l[3]);
	const value = (value0 === -1) ? null : value0;
	const row = {
		step_agent: step.agent,
		step_equipment: step.equipment,
		step_program: step.program,
		step_syringe: toNumber(l[0]),
		step_labware: l[1],
		well: l[2],
		value_type: "volume",
		value_units: "ul",
		value
	};
	return row;
});
// console.log(table0)

function step1() {
	// See if we can find the logfile
	const logDir = "C:\\ProgramData\\Tecan\\EVOware\\AuditTrail\\log";
	const files = fs.readdirSync(logDir).filter(filename => (filename.indexOf("EVO_") === 0));
	if (files.length > 0) {
		files.sort();
		const logFile = files[files.length - 1];

		/*const lineReader = require('reverse-line-reader');
		lineReader.eachLine(logfile, function(line, last) {
			console.log(line);

			if ( done ) {
				return false; // stop reading
			}
		});
		*/

		const content = fs.readFileSync(path.join(logDir, logFile), "utf8");
		const matchRPZ0 = content.match(/[\S\s]*> C\d,RPZ0[\s]+- C\d,0,(\d*),(\d*),(\d*),(\d*),(\d*),(\d*),(\d*),(\d*)/);
		const matchRVZ1 = content.match(/[\S\s]*> C\d,RVZ1[\s]+.{14}- C\d,0,(\d*),(\d*),(\d*),(\d*),(\d*),(\d*),(\d*),(\d*)/);
		if (matchRPZ0 || matchRVZ1) {
			// const match = matches[matches.length - 1].match(/> C\d,RPZ0[\s]+- C\d,0,(\d*),(\d*),(\d*),(\d*),(\d*),(\d*),(\d*),(\d*)/);
			table0.forEach(row => {
				if (matchRPZ0) {
					const z = Number(matchRPZ0[row.step_syringe]);
					if (!_isNaN(z) && row.value > 0) {
						row.value3_type = "zlevel";
						row.value3 = z;
					}
				}
				if (matchRVZ1) {
					const z = Number(matchRVZ1[row.step_syringe]);
					if (!_isNaN(z) && row.value > 0) {
						row.value2_type = "zlevel";
						row.value2 = z;
					}
				}
			});
		}
		// `C:\ProgramData\Tecan\EVOware\AuditTrail\log`, we might use a regular expression such as this:
		// `content.match(/> C\d,RPZ0[\s]+- C\d,0,(\d*),(\d*),(\d*),(\d*),(\d*),(\d*),(\d*),(\d*)/)`
		// `content.match(/> C\d,SML(\d*),(\d*),(\d*),(\d*),(\d*),(\d*),(\d*),(\d*)/)`
	}

	step2();
}

function step2() {
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

	measurementHelpers.handleOutputWriteTo(output, table, runDir, prefix);
	measurementHelpers.handleOutputAppendTo(output, table, runDir);

	process.exit(0);
}

step1();
