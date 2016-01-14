var _ = require('lodash');
var should = require('should');
import * as EvowareCarrierFile from '../src/evoware/EvowareCarrierFile.js';
import * as EvowareTableFile from '../src/evoware/EvowareTableFile.js';

describe('EvowareTableFile', function() {
	describe('load', function () {
		it('should load table from NewLayout_Feb2015.ewt', function () {
			const carrierData = EvowareCarrierFile.loadEvowareCarrierData("../testdata/bsse-mario/Carrier.cfg");
			const tableData = EvowareTableFile.load(carrierData, "../testdata/bsse-mario/NewLayout_Feb2015.ewt");
			//console.log(JSON.stringify(tableData, null, '\t'));
			//result.printCarriersById();
			//should.deepEqual(_.isEmpty(result), false);
		});
	});

	describe('toString_carriers', function() {
		it("should stringify a table with system liquid and wash station", function() {
			const carrierData = EvowareCarrierFile.loadEvowareCarrierData("../testdata/bsse-mario/Carrier.cfg");
			/*const carrierData = {
				nameToCarrier
			}*/
			const table = {
				"System": {
					"-1": {
						"external": { "n1": 4, "n2": 0 }
					}
				},
				"Wash Station Clean": {
					"1": {
						"0": { "labwareModelName": "Wash Station Cleaner shallow" },
						"1": { "labwareModelName": "Wash Station Waste" },
						"2": { "labwareModelName": "Wash Station Cleaner deep" },
						"internal": true
					}
				},
			};

			const s = EvowareTableFile.toString_carriers(carrierData, table);
			should.deepEqual(s, "14;-1;239;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;");
		});
	});
});
