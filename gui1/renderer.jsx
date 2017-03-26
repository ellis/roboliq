'use babel'

import _ from 'lodash';
import {Map, List, fromJS, toJS} from 'immutable';

import rendererReducer from './reducer.js';

// Import Views
import Protocol from './views/Protocol.jsx';
// import constants from './views/constants.jsx';
// import ListsList from './views/ListsList.jsx';
// import Menu from './views/Menu.jsx';
// import Center from './views/Center.jsx';
// import ProductList from './views/ProductList.jsx';

// This file is required by the index.html file and will
// be executed in the renderer process for that window.
// All of the Node.js APIs are available in this process.

const ipc = require('electron').ipcRenderer;

ipc.on("loadProtocols", function (event, result) {
	console.log("loadProtocols:");
	console.log({result});
	store.dispatch({type: 'SET_PROTOCOLS', filenames: result.filenames, protocols: result.protocols});
});

const { createStore } = Redux;
const store = createStore(rendererReducer.reducer);

const controls = {
	onEdit: (path) => {
		store.dispatch({type: "EDIT", path});
	},
	onSetProperty: (path, value) => {
		store.dispatch({type: "SET", path, value});
	}
}

function render() {
	const state = store.getState().toJS();
	console.log("render:");
	console.log({state})
	// console.log({ui: state.ui})
	// console.log({lists: state.data.lists})
	ReactDOM.render(
		<div>
			<Protocol
				state={state}
				controls={controls}
				onEdit={controls.onEdit}
				onSetProperty={controls.onSetProperty}
			/>
		</div>,
		document.getElementById('root')
	);
};

store.subscribe(render);
render();

ipc.send("loadProtocols", {main: "../protocols/yeast-transformation-temp.yaml", output: "./example_protocol_output.json"});
