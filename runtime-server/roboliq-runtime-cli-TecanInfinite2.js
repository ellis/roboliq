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
fs.renameSync(xmlFile, baseFile+".xml");

if (wellDesignFactor !== "_") {
	const protocol = require(scriptFile);
	const factors = protocol.reports[stepId].measurementFactors;
	// console.log(factors);

	const wellToFactors = {};
	_.forEach(factors, factorRow => {
		const well = factorRow[wellDesignFactor];
		wellToFactors[well] = factorRow;
	});
	// console.log(wellToFactors);

	// For each row in the measurement table, try to add the factor columns
	const table = result.table.map(measurementRow0 => {
		const well = measurementRow0.well;
		// Omit the "well" column from the measurement row, unless the wellDesignFactor = "well"
		const measurementRow = (wellDesignFactor === "well") ? measurementRow0 : _.omit(measurementRow0, "well");
		// console.log(measurementRow)
		// Try to get the factors for this well
		const factorRow = wellToFactors[well];
		return _.merge({}, factorRow, measurementRow);
	});
	// console.log(table);

	// Save the JSON file
	fs.writeFileSync(jsonFile, JSON.stringify(table, null, '\t'));

	if (dataset !== "_" && table.length > 0) {
		const datasetFile = path.join(runDir, dataset+".json");
		const contents = table.map(s => JSON.stringify(s)).join("\n") + "\n";
		fs.appendFileSync(datasetFile, contents);
	}
}
else {
	fs.writeFileSync(jsonFile, JSON.stringify(result.table, null, '\t'));
}

//
// var packet = {type: "TecanInfinite", protocolHash: "0", runId: runId, xml: xml};
// sendPacket(packet, opts);
