const child_process = require('child_process');
const fs = require('fs');
const opts = require('commander');
const mkdirp = require('mkdirp');
const moment = require('moment');
const path = require('path');
const get = require('lodash/get');
const trim = require('lodash/trim');
// const sendPacket = require('./roboliq-runtime-sendPacket.js');

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

const protocol = require(scriptFile);
var step = get(protocol.steps, stepId);

console.log(step)

switch (step.testType) {
	case "R":
		const testFile = path.join(runDir, "execTest.R");
		fs.writeFileSync(testFile, step.test);
		try {
			console.log("A")
			var result = child_process.execFileSync(
				"C:\\Program Files\\R\\R-3.3.0\\bin\\x64\\Rscript.exe",
				// "/usr/bin/Rscript",
				["--vanilla", testFile],
				{
					cwd: runDir,
					encoding: "utf8",
					stdio: []
				}
			).toString();
			var resultLines = result.split("\n").map(trim);
			console.log({result, resultLines});
			const line = (resultLines.length > 0) ? resultLines[resultLines.length - 1] : "";
			var json = (line) ? JSON.parse(line) : undefined;
			var exitCode = (json) ? 0 : 1;
			console.log({exitCode})
			process.exit(exitCode);
		} catch (e) {
			console.log({e})
			process.exit(1);
		}

		break;
	case "nodejs":
		break;
}


//var packet = {type: "execTest", protocolHash: "0", runId: runId};
// sendPacket(packet, opts);
