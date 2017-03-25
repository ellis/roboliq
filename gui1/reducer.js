'use babel'

import _ from 'lodash';
import assert from 'assert';
import {Map, List, fromJS, toJS} from 'immutable';

// import constants from './views/constants.jsx';
// import {scoreProduct} from '../amazon/scoreFuncs.js';

const state0 = fromJS({
	configFilenames: [],
	protocolFilename: "./example_protocol_output.json",
	protocolBase: {},
	protocol: {},
	editing: null
});

function reducer(state = state0, action) {
	console.log({action})
	let state1 = state;
	switch (action.type) {
		case "SET_CONFIG_FILENAMES":
			state1 = state.setIn(["configFilenames"], fromJS(action.value));
			return state1;
		case "SET_PROTOCOL_FILENAME":
			state1 = state.setIn(["protocolFilename"], fromJS(action.value));
			return state1;
		case "SET_PROTOCOL":
			state1 = state.setIn(["protocol"], fromJS(action.value));
			return state1;
		case "EDIT":
			state1 = state.setIn(["editing"], fromJS(action.path));
			console.log({editing: state1.editing});
			return state1;
		case "SET":
			state1 = state.setIn(["protocol"].concat(action.path), fromJS(action.value));
			return state1;
		default:
			return state1;
	}
	return state1;
};

module.exports = {
	reducer,
	state0
}
