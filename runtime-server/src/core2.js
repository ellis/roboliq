import _ from 'lodash';
import {List, Map, fromJS} from 'immutable';

export const INITIAL_STATE = Map();

export function setProtocol(state, protocol) {
	return state.set("protocol", fromJS(protocol));
}

export function setStepTime(state, time, begins, ends) {
	_.forEach(begins, step => {
		state = state.setIn(["timing", step, "from"], time);
	});
	_.forEach(ends, step => {
		state = state.setIn(["timing", step, "till"], time);
	});
	return state;
}
