var socket = require('socket.io-client')('http://localhost:8081');

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

socket.on('connect', function(){
	socket.emit("action", {type: "setStepTime", time: formatLocalDate(), begins: ["1", "1.1"], ends: []});
});

socket.on('state', function(data){console.log(data)});

socket.on('disconnect', function(){
	process.exit(0);
});
