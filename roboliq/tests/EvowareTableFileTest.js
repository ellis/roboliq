var _ = require('lodash');
var should = require('should');
import * as EvowareCarrierFile from '../src/evoware/EvowareCarrierFile.js';
import * as EvowareTableFile from '../src/evoware/EvowareTableFile.js';

describe('EvowareTableFile', function() {
	describe('load', function () {
		it('should load table from NewLayout_Feb2015.ewt', function () {
			const carrierData = EvowareCarrierFile.loadEvowareCarrierData("../testdata/bsse-mario/Carrier.cfg");
			const tableData = EvowareTableFile.load(carrierData, "../testdata/bsse-mario/NewLayout_Feb2015.ewt");
			console.log(table)
			//result.printCarriersById();
			should.deepEqual(_.isEmpty(result), false);
		});
	});
});
