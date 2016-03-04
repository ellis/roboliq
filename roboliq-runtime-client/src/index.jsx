import React from 'react';
import ReactDOM from 'react-dom';
import {Router, Route, hashHistory} from 'react-router';
import {createStore, applyMiddleware} from 'redux';
import {Provider} from 'react-redux';
import io from 'socket.io-client';
import reducer from './reducer';
import {setClientId, setState, setConnectionState} from './action_creators';
import remoteActionMiddleware from './remote_action_middleware';
import getClientId from './client_id';
import App from './components/App';
import {RuntimeContainer} from './components/Runtime';
import {ResultsContainer} from './components/Results';
import {VotingContainer} from './components/Voting';

require('./style.css');

const socket = io(`${location.protocol}//${location.hostname}:12345`);
socket.on('state', state =>
  store.dispatch(setState(state))
);
[
  'connect',
  'connect_error',
  'connect_timeout',
  'reconnect',
  'reconnecting',
  'reconnect_error',
  'reconnect_failed'
].forEach(ev =>
  socket.on(ev, () => store.dispatch(setConnectionState(ev, socket.connected)))
);

const createStoreWithMiddleware = applyMiddleware(
  remoteActionMiddleware(socket)
)(createStore);
const store = createStoreWithMiddleware(reducer);
store.dispatch(setClientId(getClientId()));

const routes = <Route component={App}>
  <Route path="/" component={RuntimeContainer} />
  <Route path="/vote" component={VotingContainer} />
  <Route path="/results" component={ResultsContainer} />
</Route>;

ReactDOM.render(
  <Provider store={store}>
    <Router history={hashHistory}>{routes}</Router>
  </Provider>,
  document.getElementById('app')
);
