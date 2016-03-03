import makeStore from './src/store';
import {startServerRuntime} from './src/serverRuntime.js';
import {startServerUi} from './src/serverUi.js';

export const store = makeStore();
startServerRuntime(store);
startServerUi(store);

store.dispatch({
  type: 'SET_ENTRIES',
  entries: require('./entries.json')
});
store.dispatch({type: 'NEXT'});
