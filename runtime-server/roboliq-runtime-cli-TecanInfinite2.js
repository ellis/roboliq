// console.log("C")
import _ from 'lodash';
var fs = require('fs');
var opts = require('commander');
import mkdirp from 'mkdirp';
var moment = require('moment');
var path = require('path');
// var sendPacket = require('./roboliq-runtime-sendPacket.js');
// console.log("D")
import processXml from './src/formats/TecanInfinite.js';
// console.log("E")

opts
	.version("1.0")
	.option("--no-rename", "don't rename the xmlfile")
	.arguments("<script> <step> <xmlfile> <factor> <dataset>")
	.parse(process.argv);

// console.log(opts);

const [scriptFile, stepId, xmlFile, wellDesignFactor, dataset] = opts.args;
// console.log({scriptFile, stepId, xmlFile, wellDesignFactor, dataset})

const scriptDir = path.dirname(scriptFile);
const runFile = path.join(scriptDir, path.basename(scriptFile, ".out.json")+".run");
const runId = _.trim(fs.readFileSync(runFile));
const runDir = path.join(scriptDir, runId);

// Make sure the run directory exists
mkdirp.sync(runDir);

const result = processXml(xmlFile);

const date = moment(result.date);
const prefix = date.format("YYYYMMDD_HHmmss")+"-"+stepId+"-"; // TODO: might need to change this to local time/date
const baseFile = path.join(runDir, prefix+path.basename(xmlFile, ".xml"));
const jsonFile = baseFile+".json";
// console.log(result.table);
// console.log({prefix, baseFile, jsonFile});

// Rename the measurement file
if (opts.rename) {
	fs.renameSync(xmlFile, baseFile+".xml");
}

const protocol = require(scriptFile);
const factors = _.get(protocol.reports, [stepId, "measurementFactors"]);
const step = _.get(protocol.steps, stepId);
const userValues = _.get(step, "program.userValues");
const objectName = step.object;
// console.log(factors);

// HACK: copied from design2.js
function getCommonValues(table) {
	if (_.isEmpty(table)) return {};

	let common = _.clone(table[0]);
	for (let i = 1; i < table.length; i++) {
		// Remove any value from common which aren't shared with this row.
		_.forEach(table[i], (value, name) => {
			if (common.hasOwnProperty(name) && !_.isEqual(common[name], value)) {
				delete common[name];
			}
		});
	}

	return common;
}

let table;
if (wellDesignFactor !== "_") {
	const wellToFactors = {};
	_.forEach(factors, factorRow => {
		const well = factorRow[wellDesignFactor];
		wellToFactors[well] = factorRow;
	});
	// console.log(wellToFactors);

	// For each row in the measurement table, try to add the factor columns
	table = result.table.map(measurementRow0 => {
		const well = measurementRow0.well;
		// Omit the "well" column from the measurement row, unless the wellDesignFactor = "well"
		const measurementRow = (wellDesignFactor === "well") ? measurementRow0 : _.omit(measurementRow0, "well");
		// console.log(measurementRow)
		// Try to get the factors for this well
		const factorRow = wellToFactors[well];
		return _.merge({RUNID: runId, object: objectName}, factorRow, userValues, measurementRow);
	});
	// console.log(table);
}
else {
	const common = getCommonValues(factors);
	// For each row in the measurement table, try to add the factor columns
	table = result.table.map(measurementRow => {
		// console.log(measurementRow)
		return _.merge({RUNID: runId, object: objectName}, common, userValues, measurementRow);
	});
}

// Save the JSON file
fs.writeFileSync(jsonFile, JSON.stringify(table, null, '\t'));

if (dataset !== "_" && !_.isEmpty(table)) {
	const datasetFile = path.join(runDir, dataset+".jsonl");
	const contents = table.map(s => JSON.stringify(s)).join("\n") + "\n";
	fs.appendFileSync(datasetFile, contents);
}

console.log("done")
//
// var packet = {type: "TecanInfinite", protocolHash: "0", runId: runId, xml: xml};
// sendPacket(packet, opts);