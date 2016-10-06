import assert from 'assert';
import makeStore from './src/store';
import {startServerRuntime} from './src/serverRuntime.js';
import {startServerUi} from './src/serverUi.js';

export const store = makeStore();
startServerRuntime(store);
startServerUi(store);

const protocolPath = process.argv[2];
assert(protocolPath)
const protocol = require(protocolPath);

store.dispatch({
  type: "setProtocol",
  protocol: protocol || {}
});
