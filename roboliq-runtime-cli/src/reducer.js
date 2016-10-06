import {setEntries, INITIAL_STATE} from './core';
import {setProtocol, setStepTime} from './core2.js';

export default function reducer(state = INITIAL_STATE, action) {
	switch (action.type) {
	// case "load":
	// 	return load()
	case "setProtocol":
		return setProtocol(state, action.protocol);
	case "setStepTime":
		return setStepTime(state, action.time, action.begins, action.ends);
	case 'SET_ENTRIES':
		return setEntries(state, action.entries);
	}
	return state;
}
