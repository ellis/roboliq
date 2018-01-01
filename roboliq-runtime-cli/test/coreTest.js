const {List, Map, fromJS} = require('immutable');
const {expect} = require('chai');

const {setProtocol, setStepTime} = require('../src/core2.js');

const {protocol1} = require('./protocolExamples.js');

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
					{time: "2015-01-01T00:00:00Z", type: 0, step: "1"},
					{time: "2015-01-01T00:00:00Z", type: 0, step: "1.1"},
				]
			}));

			state = setStepTime(state, "2015-01-01T00:01:00Z", ["2", "2.1"], ["1", "1.1"]);
			expect(state).to.equal(fromJS({
				protocol: protocol1,
				timing: [
					{time: "2015-01-01T00:00:00Z", type: 0, step: "1"},
					{time: "2015-01-01T00:00:00Z", type: 0, step: "1.1"},
					{time: "2015-01-01T00:01:00Z", type: 1, step: "1"},
					{time: "2015-01-01T00:01:00Z", type: 1, step: "1.1"},
					{time: "2015-01-01T00:01:00Z", type: 0, step: "2"},
					{time: "2015-01-01T00:01:00Z", type: 0, step: "2.1"},
				]
			}));

			state = setStepTime(state, "2015-01-01T00:02:00Z", [], ["2", "2.1"]);
			expect(state).to.equal(fromJS({
				protocol: protocol1,
				timing: [
					{time: "2015-01-01T00:00:00Z", type: 0, step: "1"},
					{time: "2015-01-01T00:00:00Z", type: 0, step: "1.1"},
					{time: "2015-01-01T00:01:00Z", type: 1, step: "1"},
					{time: "2015-01-01T00:01:00Z", type: 1, step: "1.1"},
					{time: "2015-01-01T00:01:00Z", type: 0, step: "2"},
					{time: "2015-01-01T00:01:00Z", type: 0, step: "2.1"},
					{time: "2015-01-01T00:02:00Z", type: 1, step: "2"},
					{time: "2015-01-01T00:02:00Z", type: 1, step: "2.1"},
				]
			}));

		});
	});
});
