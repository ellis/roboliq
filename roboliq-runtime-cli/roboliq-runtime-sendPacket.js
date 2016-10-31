var fs = require('fs');
var path = require('path');
var socket = require('socket.io-client')('http://localhost:12346');

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

function sendPacket(packet, opts) {
	var logdir = opts.logdir || __dirname;
	var logfile = path.join(logdir, "roboliq-runtime-cli.log");

	packet.time = formatLocalDate();

	fs.appendFile(logfile, JSON.stringify(packet)+"\n");

	socket.on('connect', function() {
		socket.emit("actionThenDisconnect", packet);
	});

	socket.on('state', function(data) {
		console.log(data)
	});

	socket.on('disconnect', function() { console.log("DONE"); process.exit(0); });
	socket.on('connect_error', function() { console.log("CONNECT ERROR"); process.exit(-1); });
	socket.on('connect_timeout', function() { console.log("CONNECT TIMEOUT"); process.exit(-1); });
}

module.exports = sendPacket;
