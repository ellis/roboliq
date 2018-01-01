/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 *
 * NOTE: This module is not used yet.  It was started for creating an immutable
 * form of the protocol.
 */

const _ = require('lodash');
const {List, Map, fromJS} = require('immutable');

const INITIAL_STATE = Map();

function addProtocol(state, filename, contents, filecache) {
	const n = state.get("protocolInputs", Map()).count();
	state = state.setIn(["protocolInputs", (n+1).toString()], fromJS({filename, contents}));
	state = state.mergeIn("filecache")
	const protocol0 = state.get("protocol", Map()).toJS();
	const protocol = loadProtocol()
}

function setProtocol(state, protocol) {
	return state.set("protocol", fromJS(protocol));
}

function setStepTime(state, time, begins, ends) {
	_.forEach(ends, step => {
		state = state.updateIn(["timing"], List(), l => l.push(Map({time, "type": 1, step})));
	});
	_.forEach(begins, step => {
		state = state.updateIn(["timing"], List(), l => l.push(Map({time, "type": 0, step})));
	});
	return state;
}

module.exports = {
	INITIAL_STATE,
	addProtocol,
	setProtocol,
	setStepTime,
};
