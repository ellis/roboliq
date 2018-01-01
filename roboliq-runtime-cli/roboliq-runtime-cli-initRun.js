const fs = require('fs');
const opts = require('commander');
const mkdirp = require('mkdirp');
const moment = require('moment');
const path = require('path');
// const sendPacket = require('./roboliq-runtime-sendPacket.js');

opts
	.version("1.0")
	.arguments("<script>")
	.parse(process.argv);

// console.log(opts);

const scriptFile = opts.args[0];
const scriptDir = path.dirname(scriptFile);
const runFile = path.join(scriptDir, path.basename(scriptFile, ".out.json")+".run");
const runId = moment().format("YYYYMMDD_HHmmss");
const runDir = path.join(scriptDir, runId);

mkdirp.sync(runDir);
fs.writeFileSync(runFile, runId);

var packet = {type: "initRun", protocolHash: "0", runId: runId};
// sendPacket(packet, opts);
