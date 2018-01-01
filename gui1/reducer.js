'use babel'

const _ = require('lodash');
const assert = require('assert');
import {Map, List, fromJS, toJS} from 'immutable';

// const constants = require('./views/constants.jsx');
// const {scoreProduct} = require('../amazon/scoreFuncs.js');

const state0 = fromJS({
	filenames: {},
	protocols: {},
	editing: null
});

function reducer(state = state0, action) {
	console.log({action})
	let state1 = state;
	switch (action.type) {
		// case "SET_CONFIG_FILENAMES":
		// 	state1 = state.setIn(["configFilenames"], fromJS(action.value));
		// 	return state1;
		case "SET_PROTOCOL_FILENAMES":
			state1 = state
				.setIn(["filenames"], fromJS(action.filenames))
				.deleteIn(["protocols"]);
			return state1;
		case "SET_PROTOCOLS":
			state1 = state
				.setIn(["filenames"], fromJS(action.filenames))
				.setIn(["protocols"], fromJS(action.protocols));
			return state1;
		// case "SET_PROTOCOL_COMPLETE":
		// 	state1 = state.setIn(["protocolComplete"], fromJS(action.value));
		// 	return state1;
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
