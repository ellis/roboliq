const fs = require('fs');
const Server = require('socket.io');

function startServerRuntime(store) {
	const io = new Server().attach(12346);

	io.on('connection', (socket) => {
		console.log({id: socket.id})
		socket.on("actionThenDisconnect", action => {
			fs.appendFile(__dirname+"/roboliq-runtime-server.log", JSON.stringify(action)+"\n");
			store.dispatch(action);
			socket.disconnect();
		});
		// socket.on("load", action => {
		// 	fs.appendFile(__dirname+"/roboliq-runtime-server.log", JSON.stringify(action)+"\n");
		// 	store.dispatch(action);
		// 	socket.disconnect();
		// });
	});
}

module.exports = {
  startServerRuntime,
};
