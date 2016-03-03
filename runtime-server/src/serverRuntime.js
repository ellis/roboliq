import Server from 'socket.io';

export function startServerRuntime(store) {
	const io = new Server().attach(12346);

	io.on('connection', (socket) => {
		console.log({id: socket.id})
		socket.on("actionThenDisconnect", action => {
			store.dispatch(action);
			socket.disconnect();
		});
	});
}
