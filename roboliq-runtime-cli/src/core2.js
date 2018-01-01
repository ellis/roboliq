const _ = require('lodash');
const {List, Map, fromJS} = require('immutable');

const INITIAL_STATE = Map();

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
  setProtocol,
  setStepTime,
};
