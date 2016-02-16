import React from 'react';
import ReactDOM from 'react-dom';
//import {IndexRoute, Route, Router, browserHistory} from 'react-router';
import {Route, Router} from 'react-router';
import {createStore} from 'redux';
import {Provider} from 'react-redux';
import reducer from './reducer';
import App from './components/App';
import {DesignContainer} from './components/Design';
import Results from './components/Results';
import {VotingContainer} from './components/Voting';

const store = createStore(reducer);
store.dispatch({
	type: 'SET_STATE',
	state: {
		vote: {
			pair: [ 'Sunshine', '28 Days Later' ],
			tally: { Sunshine: 2 }
		},
		design: {
			"conditions": {
				"strainSource": "strain1",
				"mediaSource": "media1",
				"cultureNum*=range": {
					"till": 96
				},
				"cultureWell=range": {
					"till": 96,
					"random": true
				},
				"syringe=range": {
					"till": 8,
					"random": true,
					"rotateValues": true
				}
			},
			"actions": []
		},
		designText: `{
			"conditions": {
				"strainSource": "strain1",
				"mediaSource": "media1",
				"cultureNum*=range": {
					"till": 96
				},
				"cultureWell=range": {
					"till": 96,
					"random": true
				},
				"syringe=range": {
					"till": 8,
					"random": true,
					"rotateValues": true
				}
			},
			"actions": []
		}`,
	}
});

const pair = ['Trainspotting', '28 Days Later'];

/*
const router = (
	<Router>
		<Route path="/" component={App}>
			<IndexRoute component={Results}/>
			<Route path="voting" component={Voting}/>
			// <Route path="results" component={Results}/>
		</Route>
	</Router>
);
*/
const router = (
	<Router>
		<Route component={App}>
			<Route path="/results" component={Results}/>
			<Route path="/design" component={DesignContainer}/>
			<Route path="/" component={VotingContainer}/>
			// <Route path="results" component={Results}/>
		</Route>
	</Router>
);

ReactDOM.render(<Provider store={store}>{router}</Provider>, document.getElementById('app'));
