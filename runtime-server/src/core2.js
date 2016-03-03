import _ from 'lodash';
import {List, Map, fromJS} from 'immutable';

export const INITIAL_STATE = Map();

export function setProtocol(state, protocol) {
	return state.set("protocol", fromJS(protocol));
}

export function setStepTime(state, time, begins, ends) {
	_.forEach(ends, step => {
		state = state.updateIn(["timing"], List(), l => l.push(Map({time, step, "type": 1})));
	});
	_.forEach(begins, step => {
		state = state.updateIn(["timing"], List(), l => l.push(Map({time, step, "type": 0})));
	});
	return state;
}
