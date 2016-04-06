var fs = require('fs');
var opts = require('commander');
var path = require('path');
var socket = require('socket.io-client')('http://localhost:12346');

opts
	.version("1.0")
	.option("--begin [step]", "indicate the beginning of the given step")
	.option("--end [step]", "indicate the end of the given step")
	.option("--logdir [dir]", "directory where log file should be written")
	.parse(process.argv);

function formatLocalDate() {
	var now = new Date(),
		tzo = -now.getTimezoneOffset(),
		dif = tzo >= 0 ? '+' : '-',
		pad = function(num) {
			var norm = Math.abs(Math.floor(num));
			return (norm < 10 ? '0' : '') + norm;
		};
	return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate()) + 'T' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds()) + dif + pad(tzo / 60) + ':' + pad(tzo % 60);
}

if (opts.hasOwnProperty("begin") || opts.hasOwnProperty("end")) {
	var begins = (opts.begin || "").split(",");
	var ends = (opts.end || "").split(",");
	var logdir = opts.logdir || __dirname;
	var logfile = path.join(logdir, "roboliq-runtime-cli.log");

	var packet = {type: "setStepTime", time: formatLocalDate(), begins: begins, ends: ends};
	fs.appendFile(logfile, JSON.stringify(packet)+"\n");

	socket.on('connect', function() {
		socket.emit("actionThenDisconnect", packet);
	});

	socket.on('state', function(data) {
		console.log(data)
	});

	socket.on('disconnect', function() {
		process.exit(0);
	});
}
