console.log("C")
import _ from 'lodash';
var fs = require('fs');
var opts = require('commander');
import mkdirp from 'mkdirp';
var moment = require('moment');
var path = require('path');
// var sendPacket = require('./roboliq-runtime-sendPacket.js');
console.log("D")
import processXml from './src/formats/TecanInfinite.js';
console.log("E")

opts
	.version("1.0")
	.arguments("<script> <step> <datafile>")
	.parse(process.argv);

// console.log(opts);

const scriptFile = opts.args[0];
const stepId = opts.args[1];
const xmlFile = opts.args[2];

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
console.log(result.table); console.log({prefix, baseFile, jsonFile});

// Rename the measurement file
// fs.renameSync(xmlFile, baseFile+".xml");
//
// var packet = {type: "TecanInfinite", protocolHash: "0", runId: runId, xml: xml};
// sendPacket(packet, opts);
