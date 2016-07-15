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
import designHelper from './src/designHelper.js';
// console.log("E")

opts
	.version("1.0")
	.option("--no-rename", "don't rename the xmlfile")
	.arguments("<script> <step> <xmlfile>")
	.parse(process.argv);

// console.log(opts);

const [scriptFile, stepId, xmlFile] = opts.args;
// console.log({scriptFile, stepId, xmlFile})

const scriptBase = path.basename(scriptFile, ".out.json");
const scriptDir = path.dirname(scriptFile);
// console.log({scriptDir})
const runFile = path.join(scriptDir, scriptBase+".run");
// console.log({runFile})
const runId = _.trim(fs.readFileSync(runFile));
const runDir = path.join(scriptDir, runId);

// Make sure the run directory exists
mkdirp.sync(runDir);

const result = processXml(xmlFile);
// console.log({result})

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
const joinKey = _.get(step, "output.joinKey", _.get(step, "program.wellDesignFactor"));
const userValues = _.get(step, "output.userValues", _.get(step, "program.userValues"));
const appendTo = _.get(step, "output.appendTo", _.get(step, "outputDataset"));
const objectName = step.object;
// console.log(factors);
// console.log({joinKey, appendTo})

let table;
if (!_.isEmpty(joinKey)) {
	const wellToFactors = {};
	_.forEach(factors, factorRow => {
		const well = factorRow[joinKey];
		wellToFactors[well] = factorRow;
	});
	// console.log(wellToFactors);

	// For each row in the measurement table, try to add the factor columns
	table = result.table.map(measurementRow0 => {
		const well = measurementRow0.well;
		// Omit the "well" column from the measurement row, unless the joinKey = "well"
		const measurementRow = (joinKey === "well") ? measurementRow0 : _.omit(measurementRow0, "well");
		// console.log(measurementRow)
		// Try to get the factors for this well
		const factorRow = wellToFactors[well];
		return _.merge({RUNID: runId, object: objectName}, factorRow, userValues, measurementRow);
	});
	// console.log(table);
}
else {
	const common = designHelper.getCommonValues(factors);
	// For each row in the measurement table, try to add the factor columns
	table = result.table.map(measurementRow => {
		// console.log(measurementRow)
		return _.merge({RUNID: runId, object: objectName}, common, userValues, measurementRow);
	});
}

// Save the JSON file
fs.writeFileSync(jsonFile, JSON.stringify(table, null, '\t'));

if (!_.isEmpty(appendTo) && !_.isEmpty(table)) {
	const appendToFile = path.join(runDir, appendTo+".jsonl");
	const contents = table.map(s => JSON.stringify(s)).join("\n") + "\n";
	fs.appendFileSync(appendToFile, contents);

	// HACK to copy data to shared drive
	if (true) {
		const hackDir = path.join("Y:\\Projects\\Roboliq\\labdata", scriptBase, runId);
		console.log({hackDir})
		mkdirp.sync(hackDir);
		const hackFile = path.join(hackDir, appendTo+".jsonl");
		console.log("writing duplicate file to: "+hackFile)
		const contents = table.map(s => JSON.stringify(s)).join("\n") + "\n";
		fs.appendFileSync(hackFile, contents);
	}
}

console.log("done")
//
// var packet = {type: "TecanInfinite", protocolHash: "0", runId: runId, xml: xml};
// sendPacket(packet, opts);
