const assert = require('assert');
const makeStore = require('./src/store');
const {startServerRuntime} = require('./src/serverRuntime.js');
const {startServerUi} = require('./src/serverUi.js');

const store = makeStore();
startServerRuntime(store);
startServerUi(store);

const protocolPath = process.argv[2];
assert(protocolPath)
const protocol = require(protocolPath);

store.dispatch({
  type: "setProtocol",
  protocol: protocol || {}
});

module.exports = {
  store,
};
