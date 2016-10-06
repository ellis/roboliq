var _ = require('lodash');
var should = require('should');
import * as EvowareCarrierFile from '../src/evoware/EvowareCarrierFile.js';

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
