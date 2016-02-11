import {createStore} from 'redux';
import reducer from './reducer';

export default function makeStore() {
  return createStore(reducer);
}

CONTINUE: "So, the Redux store ties things together into something we'll be able to use as the central point of our application: It holds the current state, and over time can"
