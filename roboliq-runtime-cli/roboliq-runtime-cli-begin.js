const opts = require('commander');
const sendPacket = require('./roboliq-runtime-sendPacket.js');

opts
	.version("1.0")
	.option("--step [step]", "step ID")
	.option("--logdir [dir]", "directory where log file should be written")
	.parse(process.argv);

console.log(opts);
var begins = (opts.step || "").split(",");
var packet = {type: "setStepTime", begins: begins};
sendPacket(packet, opts);
