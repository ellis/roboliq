var fs = require('fs');
var opts = require('commander');
var moment = require('moment')
var path = require('path');
var sendPacket = require('./roboliq-runtime-sendPacket.js');

opts
	.version("1.0")
	.arguments("<script>")
	.parse(process.argv);

// console.log(opts);

const scriptFile = opts.args[0];
const scriptDir = path.dirname(scriptFile);
const runFile = path.join(scriptDir, path.basename(scriptFile, ".out.json")+".run");
const runId = moment().format("YYYMMDD_HHmmss");
const runDir = path.join(scriptDir, runId);

fs.writeFileSync(runFile, runId);
fs.mkdirSync(runDir);

var packet = {type: "initRun", protocolHash: "0", runId: runId};
sendPacket(packet, opts);
