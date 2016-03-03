import {List, Map, fromJS} from 'immutable';
import {expect} from 'chai';

import {setProtocol, setStepTime} from '../src/core2.js';

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
};

describe('core logic', () => {

	describe('setProtocol', () => {

		it('adds the protocol to the state', () => {
			const state0 = Map();
			const state = setProtocol(state0, protocol1);
			expect(state).to.equal(fromJS({
				protocol: protocol1
			}));
		});

	});

});
