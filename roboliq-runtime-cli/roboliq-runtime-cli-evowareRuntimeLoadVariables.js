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

console.log(step)

let varset;

// If we should optionally run a script to generate the varset
switch (step.scriptType) {
	case "R":
		const testFile = path.join(runDir, "execTest.R");
		fs.writeFileSync(testFile, step.script);
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
			varset = (line) ? JSON.parse(line) : undefined;
		} catch (e) {
			console.log({e})
			process.exit(1);
		}

		break;
	case "nodejs":
		break;
}

// Run roboliq
{
	console.log("A")
	var result = child_process.execFileSync(
		// "C:\\Program Files\\R\\R-3.3.0\\bin\\x64\\Rscript.exe",
		"/usr/bin/node",
		[
			"src/main.js",
			"--evoware",
			protocol.COMPILER.roboliqOpts.evoware[0],
			path.join(scriptDir, stepId+".dump.json")
		],
		{
			cwd: path.resolve(__dirname, "../roboliq/"),
			encoding: "utf8",
			stdio: []
		}
	).toString();
	var resultLines = result.split("\n").map(trim);
	console.log({result, resultLines});
	const line = (resultLines.length > 0) ? resultLines[resultLines.length - 1] : "";
	varset = (line) ? JSON.parse(line) : undefined;
}

//var packet = {type: "execTest", protocolHash: "0", runId: runId};
// sendPacket(packet, opts);
