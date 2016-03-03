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

	describe("setStepTime", () => {
		it("adds step times to the state", () => {
			let state = Map();
			state = setProtocol(state, protocol1);

			state = setStepTime(state, "2015-01-01T00:00:00Z", ["1", "1.1"], []);
			expect(state).to.equal(fromJS({
				protocol: protocol1,
				timing: [
					{time: "2015-01-01T00:00:00Z", step: "1", type: 0},
					{time: "2015-01-01T00:00:00Z", step: "1.1", type: 0},
				]
			}));

			state = setStepTime(state, "2015-01-01T00:01:00Z", ["2", "2.1"], ["1", "1.1"]);
			expect(state).to.equal(fromJS({
				protocol: protocol1,
				timing: [
					{time: "2015-01-01T00:00:00Z", step: "1", type: 0},
					{time: "2015-01-01T00:00:00Z", step: "1.1", type: 0},
					{time: "2015-01-01T00:01:00Z", step: "1", type: 1},
					{time: "2015-01-01T00:01:00Z", step: "1.1", type: 1},
					{time: "2015-01-01T00:01:00Z", step: "2", type: 0},
					{time: "2015-01-01T00:01:00Z", step: "2.1", type: 0},
				]
			}));

			state = setStepTime(state, "2015-01-01T00:02:00Z", [], ["2", "2.1"]);
			expect(state).to.equal(fromJS({
				protocol: protocol1,
				timing: [
					{time: "2015-01-01T00:00:00Z", step: "1", type: 0},
					{time: "2015-01-01T00:00:00Z", step: "1.1", type: 0},
					{time: "2015-01-01T00:01:00Z", step: "1", type: 1},
					{time: "2015-01-01T00:01:00Z", step: "1.1", type: 1},
					{time: "2015-01-01T00:01:00Z", step: "2", type: 0},
					{time: "2015-01-01T00:01:00Z", step: "2.1", type: 0},
					{time: "2015-01-01T00:02:00Z", step: "2", type: 1},
					{time: "2015-01-01T00:02:00Z", step: "2.1", type: 1},
				]
			}));

		});
	});
});
