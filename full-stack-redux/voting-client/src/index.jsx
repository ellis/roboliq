import React from 'react';
import ReactDOM from 'react-dom';
//import {IndexRoute, Route, Router, browserHistory} from 'react-router';
import {Route, Router} from 'react-router';
import App from './components/App';
import Design from './components/Design';
import Results from './components/Results';
import Voting from './components/Voting';

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
			<Route path="/design" component={Design}/>
			<Route path="/" component={Voting}/>
			// <Route path="results" component={Results}/>
		</Route>
	</Router>
);

ReactDOM.render(router, document.getElementById('app'));
