var opts = require('commander');
var sendPacket = require('./roboliq-runtime-sendPacket.js');

opts
	.version("1.0")
	.option("--step step", "step ID")
	.option("--logdir [dir]", "directory where log file should be written");

opts
	.command("begin", "log beginning of step")
	.command("end", "log ending of step")
	.command("initRun", "initialize a new protocol run (should be called at the beginning of a protocol)")
	.command("execTest", "execute a run-time test, and return DOS exit code 0 for true/success and 1 for false/failure")
	.command("zlevel", "log z-levels")
	.command("TecanInfinite", "process Tecan Infinite measurements");

opts
	.parse(process.argv);
