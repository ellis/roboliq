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

var action;
var begins = [];
var ends = [];

for (var i = 2; i < process.argv.length; i++) {
  var step = process.argv[i];
  if (step === "begin" || step === "end") {
    action = step;
  }
  else {
    if (action === "begin") {
      begins.push(step);
    }
    else if (action === "end") {
      ends.push(step);
    }
    else {
      console.log("ERROR: step given without 'begin' or 'end' command")
    }
  }
}

socket.on('connect', function() {
	socket.emit("actionThenDisconnect", {type: "setStepTime", time: formatLocalDate(), begins: ["1", "1.1"], ends: []});
});

socket.on('state', function(data) {
	console.log(data)
});

socket.on('disconnect', function() {
	process.exit(0);
});
