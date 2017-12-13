/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

import _ from 'lodash';
import {List, Map, fromJS} from 'immutable';

export const INITIAL_STATE = Map();

export function addProtocol(state, filename, contents, filecache) {
	const n = state.get("protocolInputs", Map()).count();
	state = state.setIn(["protocolInputs", (n+1).toString()], fromJS({filename, contents}));
	state = state.mergeIn("filecache")
	const protocol0 = state.get("protocol", Map()).toJS();
	const protocol = loadProtocol()
}
export function setProtocol(state, protocol) {
	return state.set("protocol", fromJS(protocol));
}

export function setStepTime(state, time, begins, ends) {
	_.forEach(ends, step => {
		state = state.updateIn(["timing"], List(), l => l.push(Map({time, "type": 1, step})));
	});
	_.forEach(begins, step => {
		state = state.updateIn(["timing"], List(), l => l.push(Map({time, "type": 0, step})));
	});
	return state;
}
