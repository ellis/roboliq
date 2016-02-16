import {Map, fromJS} from 'immutable';
import {flattenDesign} from './design.js';
import YAML from 'js-yaml';

function setState(state, newState) {
	return state.merge(newState);
}

export default function(state = Map(), action) {
	switch (action.type) {
		case 'SET_STATE':
			return setState(state, action.state);
		case 'setDesignText':
			state = state.set('designText', action.text);
			try {
				const design = JSON.parse(action.text);
				state = state.set('design', fromJS(design));
				try {
					const table = flattenDesign(design);
					state = state.set('table', fromJS(table));
				} catch (e) {
					console.log("couldn't flatten:")
					console.log(e)
				}
			} catch (e) {
				console.log("couldn't parse:")
				console.log(e)
			}
			console.log("state: "+JSON.stringify(state, null, '\t'))
			return state;
	}
	return state;
}
