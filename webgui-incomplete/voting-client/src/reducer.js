const {List, OrderedMap, Map, fromJS} = require('immutable');
const {flattenDesign} = require('./design.js');
const YAML = require('js-yaml');

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
				//const design = JSON.parse(action.text);
				const design = YAML.safeLoad(action.text);
				state = state.set('design', fromJS(design));
				try {
					const table = flattenDesign(design);
					const table2 = List(table.map(row => OrderedMap(row)));
					state = state.set('table', table2);
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
