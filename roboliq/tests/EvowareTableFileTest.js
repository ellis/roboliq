var _ = require('lodash');
var should = require('should');
import * as EvowareCarrierFile from '../src/evoware/EvowareCarrierFile.js';
import * as EvowareTableFile from '../src/evoware/EvowareTableFile.js';

const table1 = {
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
	"Shelf 32Pos Microplate": {
		"11": {
			"0": { "labwareModelName": "DM Nunc stronghold" },
			"hotel": true,
			"external": { "n1": 0, "n2": 1 }
		}
	},
	"Hotel 4Pos Transfer Grid 69": {
		"69": {
			"0": { "labwareModelName": "DM Nunc stronghold" },
			"hotel": true,
			"external": { "n1": 0, "n2": 7 }
		}
	}
};


describe('EvowareTableFile', function() {
	describe('load', function () {
		it('should load table from NewLayout_Feb2015.ewt', function () {
			const carrierData = EvowareCarrierFile.loadEvowareCarrierData("../testdata/bsse-mario/Carrier.cfg");
			const table = EvowareTableFile.load(carrierData, "../testdata/bsse-mario/NewLayout_Feb2015.ewt");
			console.log(JSON.stringify(table, null, '\t'));
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
			const s = EvowareTableFile.toString_carriers(carrierData, table1);
			should.deepEqual(s, "14;-1;239;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;");
		});
	});

	describe('toStrings_hotels', function () {
		it("should stringify a table with two hotels", function() {
			const carrierData = EvowareCarrierFile.loadEvowareCarrierData("../testdata/bsse-mario/Carrier.cfg");
			/*const carrierData = {
				nameToCarrier
			}*/
			const s = EvowareTableFile.toStrings_hotels(carrierData, table1);
			should.deepEqual(s, [
				'998;2;', '998;315;11;', '998;322;69;'
			]);
		});
	});

	describe('toStrings_externals', function () {
		it("should stringify a table with several external objects", function() {
			const carrierData = EvowareCarrierFile.loadEvowareCarrierData("../testdata/bsse-mario/Carrier.cfg");
			/*const carrierData = {
				nameToCarrier
			}*/
			const l = EvowareTableFile.toStrings_externals(carrierData, table1);
			should.deepEqual(l, [
				'998;3;',
				'998;4;0;System;',
				'998;0;1;Shelf 32Pos Microplate;',
				'998;0;7;Hotel 4Pos Transfer Grid 69;',
				'998;2;',
				'998;315;DM Nunc stronghold;',
				'998;322;DM Nunc stronghold;',
				'998;1;',
				'998;11;',
				'998;69;'
			]);
		});
	});
});
