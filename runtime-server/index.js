import makeStore from './src/store';
import {startServerRuntime} from './src/serverRuntime.js';
import {startServerUi} from './src/serverUi.js';

export const store = makeStore();
startServerRuntime(store);
startServerUi(store);

const protocol = require(process.argv[2]);

store.dispatch({
  type: "setProtocol",
  protocol: protocol || {}
});
