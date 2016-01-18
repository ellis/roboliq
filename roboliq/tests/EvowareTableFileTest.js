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

const table1Text =
"00000000\n"+
"20000101_000000 No log in       \n"+
"                                                                                                                                \n"+
"No user logged in                                                                                                               \n"+
`--{ RES }--
V;200
--{ CFG }--
999;219;32;
14;-1;239;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;
998;0;
998;3;Wash Station Cleaner shallow;Wash Station Waste;Wash Station Cleaner deep;
998;;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;2;
998;315;11;
998;322;69;
998;3;
998;4;0;System;
998;0;1;Shelf 32Pos Microplate;
998;0;7;Hotel 4Pos Transfer Grid 69;
998;2;
998;315;DM Nunc stronghold;
998;322;DM Nunc stronghold;
998;1;
998;11;
998;69;
996;0;0;
--{ RPG }--
`;

describe('EvowareTableFile', function() {
	describe('load', function () {
		it('should load table from NewLayout_Feb2015.ewt without raising an exception', function () {
			const carrierData = EvowareCarrierFile.load("../testdata/bsse-mario/Carrier.cfg");
			//carrierData.printCarriersById();
			const table = EvowareTableFile.load(carrierData, "../testdata/bsse-mario/NewLayout_Feb2015.ewt");
			//console.log(JSON.stringify(table, null, '\t'));
			//should.deepEqual(_.isEmpty(result), false);
		});
	});

	describe('toString_internalCarriers', function() {
		it("should stringify a table with system liquid and wash station", function() {
			const carrierData = EvowareCarrierFile.load("../testdata/bsse-mario/Carrier.cfg");
			//console.log(JSON.stringify(carrierData, null, '\t'));
			/*const carrierData = {
				nameToCarrier
			}*/
			const s = EvowareTableFile.toString_internalCarriers(carrierData, table1);
			should.deepEqual(s, "14;-1;239;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;");
		});
	});

	describe('toStrings_internalLabware', function() {
		it("should stringify table1", function() {
			const carrierData = EvowareCarrierFile.load("../testdata/bsse-mario/Carrier.cfg");
			//console.log({it1: carrierData.models['Wash Station Clean'], it2: carrierData.getCarrierByName('Wash Station Clean')})
			/*const carrierData = {
				nameToCarrier
			}*/
			const l = EvowareTableFile.toStrings_internalLabware(carrierData, table1);
			//console.log(JSON.stringify(l, null, '\t'));
			should.deepEqual(l[0], "998;0;");
			should.deepEqual(l[1], "998;3;Wash Station Cleaner shallow;Wash Station Waste;Wash Station Cleaner deep;");
			should.deepEqual(l[2], "998;;;;");
			should.deepEqual(_.drop(l, 3), _.times(l.length - 3, () => "998;0;"));
		});
	});

	describe('toStrings_hotels', function () {
		it("should stringify a table with two hotels", function() {
			const carrierData = EvowareCarrierFile.load("../testdata/bsse-mario/Carrier.cfg");
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
			const carrierData = EvowareCarrierFile.load("../testdata/bsse-mario/Carrier.cfg");
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

	describe('toString', function() {
		it("should stringify a table with several external objects", function() {
			const carrierData = EvowareCarrierFile.load("../testdata/bsse-mario/Carrier.cfg");
			/*const carrierData = {
				nameToCarrier
			}*/
			const expected = table1Text//.replace(/[^\r]\n/g, "\r\n");
			const s = EvowareTableFile.toString(carrierData, table1);
			//s = s.replace(/\r\n/g, "\n");
			//console.log(s);
			should.deepEqual(s, expected);
		});
	});
});
