var opts = require('commander');
var sendPacket = require('./roboliq-runtime-sendPacket.js');

opts
	.version("1.0")
	.option("--well [id]", "comma-separated list of well specifiers")
	.option("--zlevel [value]", "comma-separated list of z-level values")
	.option("--syringe [id]", "comma-separated list of syringe identifiers")
	.option("--tipModel [id]", "comma-separated list of tipModel identifiers")
	.parse(process.argv);

console.log(opts);
var begins = (opts.step || "").split(",");
var packet = {
	type: "recordZLevels",
	wells: (opts.well || "").split(","),
	zlevels: (opts.zlevel || "").split(","),
	syringes: (opts.syringe || "").split(","),
	tipModels: (opts.tipModel || "").split(",")
};
sendPacket(packet, opts);
