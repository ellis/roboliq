const {createStore} = require('redux');
const reducer = require('./reducer');

export default function makeStore() {
  return createStore(reducer);
}
