/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const should = require('should');
const EvowareCarrierFile = require('../src/EvowareCarrierFile.js');

describe('EvowareCarrierFile', function() {
	describe('load', function () {
		it('should load models from Carrier.cfg', function () {
			const carrierData = EvowareCarrierFile.load("../testdata/bsse-mario/Carrier.cfg");
			//console.log({carrierData})
			//carrierData.printCarriersById();
			should.deepEqual(_.isEmpty(carrierData), false);
		});
	});
});
