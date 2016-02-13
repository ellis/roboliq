import React from 'react';
import ReactDOM from 'react-dom';
import {IndexRoute, Route, Router, browserHistory} from 'react-router';
import App from './components/App';
import Results from './components/Results';
import Voting from './components/Voting';

const pair = ['Trainspotting', '28 Days Later'];

const router = (
	<Router history={browserHistory}>
		<Route path="/" component={App}>
			<IndexRoute component={Results}/>
			<Route path="voting" component={Voting}/>
			// <Route path="results" component={Results}/>
		</Route>
	</Router>
);

ReactDOM.render(router, document.getElementById('app'));
