// console.log("C")
const _ = require('lodash');
const fs = require('fs');
const opts = require('commander');
const mkdirp = require('mkdirp');
const moment = require('moment');
const path = require('path');
// const sendPacket = require('./roboliq-runtime-sendPacket.js');
// console.log("D")
const processXml = require('./src/formats/TecanInfinite.js');
const designHelper = require('./src/designHelper.js');
const measurementHelpers = require('./src/measurementHelpers.js');
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
const output = _.get(step, "output", {});
const joinKey = _.get(step, "output.joinKey", _.get(step, "program.wellDesignFactor"));
const userValues = _.get(step, "output.userValues", _.get(step, "program.userValues"));
const appendTo = _.get(step, "output.appendTo", _.get(step, "outputDataset"));
const outputUnits = _.get(step, "output.units");
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

// console.log({output})
measurementHelpers.handleOutputUnits(output, table);

// Save the JSON file
fs.writeFileSync(jsonFile, JSON.stringify(table, null, '\t'));

measurementHelpers.handleOutputAppendTo(output, table, runDir);

console.log("done")
//
// var packet = {type: "TecanInfinite", protocolHash: "0", runId: runId, xml: xml};
// sendPacket(packet, opts);
