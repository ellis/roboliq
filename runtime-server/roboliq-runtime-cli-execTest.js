var child_process = require('child_process');
var fs = require('fs');
var opts = require('commander');
var mkdirp = require('mkdirp');
var moment = require('moment');
var path = require('path');
var get = require('lodash/get');
var trim = require('lodash/trim');
// var sendPacket = require('./roboliq-runtime-sendPacket.js');

opts
	.version("1.0")
	.arguments("<script> <step>")
	.parse(process.argv);
// console.log(opts);

var scriptFile = opts.args[0];
var stepId = opts.args[1];

const scriptDir = path.dirname(scriptFile);
const runFile = path.join(scriptDir, path.basename(scriptFile, ".out.json")+".run");
const runId = trim(fs.readFileSync(runFile));
const runDir = path.join(scriptDir, runId);
mkdirp.sync(runDir);

var protocol = require(scriptFile);
var step = get(protocol.steps, stepId);

switch (step.testType) {
	case "R":
		const testFile = path.join(runDir, "execTest.R");
		fs.writeFileSync(testFile, step.test);
		child_process.execFileSync(
			// "C:\\Program Files\\R\\R-3.3.0\\bin\\x64\\Rscript",
			"/usr/bin/Rscript",
			["--vanilla", testFile],
			{
				cwd: runDir,
				encoding: "utf8"
			}
		);
		break;
	case "nodejs":
		break;
}


//var packet = {type: "execTest", protocolHash: "0", runId: runId};
// sendPacket(packet, opts);
