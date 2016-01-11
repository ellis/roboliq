var _ = require('lodash');
var should = require('should');
import * as EvowareCarrierFile from '../src/evoware/EvowareCarrierFile.js';

describe('EvowareCarrierFile', function() {
	describe('loadEvowareCarrierData', function () {
		it('should load models from Carrier.cfg', function () {
			const result = EvowareCarrierFile.loadEvowareCarrierData("../testdata/bsse-mario/Carrier.cfg");
			should.deepEqual(result, {});
		});
	});
});
