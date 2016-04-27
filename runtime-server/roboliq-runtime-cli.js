var opts = require('commander');
var sendPacket = require('./roboliq-runtime-sendPacket.js');

opts
	.version("1.0")
	.option("--step step", "step ID")
	.option("--logdir [dir]", "directory where log file should be written");

opts
	.command('begin', "log beginning of step")
	.command('end', "log ending of step")
	.command("zlevel", "log z-levels");

opts
	.parse(process.argv);
