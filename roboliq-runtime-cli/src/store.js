const {createStore} = require('redux');
const reducer = require('./reducer');

default function makeStore() {
  return createStore(reducer);
}

module.exports = {
  function,
};
