import {setEntries, next, restart, vote, INITIAL_STATE} from './core';
import {setProtocol, setStepTime} from './core2.js';

export default function reducer(state = INITIAL_STATE, action) {
	switch (action.type) {
	case "setProtocol":
		return setProtocol(state, action.protocol);
	case "setStepTime":
		return setStepTime(state, action.time, action.begins, action.ends);
	case 'SET_ENTRIES':
		return setEntries(state, action.entries);
	case 'NEXT':
		return next(state);
	case 'RESTART':
		return restart(state);
	case 'VOTE':
		return state.update('vote', voteState => vote(voteState, action.entry, action.clientId));
	}
	return state;
}
