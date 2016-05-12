var opts = require('commander');
var sendPacket = require('./roboliq-runtime-sendPacket.js');

opts
	.version("1.0")
	.arguments("<script>")
	.parse(process.argv);


// console.log(opts);
var ends = (opts.step || "").split(",");
var packet = {type: "setStepTime", ends: ends};
sendPacket(packet, opts);
