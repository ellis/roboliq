const {Map, fromJS} = require('immutable');
const {expect} = require('chai');

const reducer = require('../src/reducer');

const {protocol1} = require('./protocolExamples.js');

describe('reducer', () => {

	it('handles setProtocol', () => {
		const state0 = Map();
		const action = {type: 'setProtocol', protocol: protocol1};
		const state = reducer(state0, action);

		expect(state).to.equal(fromJS({
			protocol: protocol1
		}));
	});

	it('can be used with reduce', () => {
		const actions = [
			{type: 'setProtocol', protocol: protocol1},
			{type: "setStepTime", time: "2015-01-01T00:00:00Z", begins: ["1", "1.1"], ends: []}
		];
		const state = actions.reduce(reducer, Map());

		expect(state).to.equal(fromJS({
			protocol: protocol1,
			timing: [
				{time: "2015-01-01T00:00:00Z", type: 0, step: "1"},
				{time: "2015-01-01T00:00:00Z", type: 0, step: "1.1"},
			]
		}));
	});

});
