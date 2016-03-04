import makeStore from './src/store';
import {startServerRuntime} from './src/serverRuntime.js';
import {startServerUi} from './src/serverUi.js';

export const store = makeStore();
startServerRuntime(store);
startServerUi(store);

const protocol1 = {
	roboliq: "v1",
	steps: {
		1: {
			command: "system.echo",
			value: "hello",
			1: {
				command: "system._echo",
				value: "hello"
			}
		},
		2: {
			command: "system.echo",
			value: "world",
			1: {
				command: "system._echo",
				value: "world"
			}
		}
	}
}
store.dispatch({
  type: "setProtocol",
  protocol: protocol1
});
