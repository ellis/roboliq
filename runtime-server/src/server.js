import Server from 'socket.io';

export function startServer(store) {
	const io = new Server().attach(8081);

	store.subscribe(
		() => io.emit('state', store.getState().toJS())
	);

	io.on('connection', (socket) => {
		console.log({socket})
		socket.emit('state', store.getState().toJS());
		socket.on('action', store.dispatch.bind(store));
	});
}
