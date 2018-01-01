/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const should = require('should');
import * as EvowareCarrierFile from '../src/EvowareCarrierFile.js';
import * as EvowareTableFile from '../src/EvowareTableFile.js';

const table1 = {
	"System": {
		"-1": {
			"external": { "n1": 4, "n2": 0 }
		}
	},
	"Wash Station Clean": {
		"1": {
			"1": { "labwareModelName": "Wash Station Cleaner shallow" },
			"2": { "labwareModelName": "Wash Station Waste" },
			"3": { "labwareModelName": "Wash Station Cleaner deep" },
			"internal": true
		}
	},
	"Shelf 32Pos Microplate": {
		"11": {
			"1": { "labwareModelName": "DM Nunc stronghold" },
			"hotel": true,
			"external": { "n1": 0, "n2": 1 }
		}
	},
	"Hotel 4Pos Transfer Grid 69": {
		"69": {
			"1": { "labwareModelName": "DM Nunc stronghold" },
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
			should.deepEqual(table, {
				"System": {
					"-1": {
						"external": { "n1": 4, "n2": 0 }
					}
				},
				"Wash Station Clean": {
					"1": {
						"1": { "labwareModelName": "Wash Station Cleaner shallow" },
						"2": { "labwareModelName": "Wash Station Waste" },
						"3": { "labwareModelName": "Wash Station Cleaner deep" },
						"internal": true
					}
				},
				"Wash Station Dirty": {
					"2": {
						"1": { "labwareModelName": "Wash Station Cleaner shallow" },
						"2": { "labwareModelName": "Wash Station Waste" },
						"3": { "labwareModelName": "Wash Station Cleaner deep" },
						"internal": true
					}
				},
				"LI - Trough 3Pos 100ml": {
					"3": {
						// "3": { "label": "Bleach", "labwareModelName": "Trough 25ml Max. Recovery" },
						"2": { "label": "Labware7", "labwareModelName": "Trough 25ml Max. Recovery" },
						"internal": true
					}
				},
				"Cooled 8Pos*15ml 8Pos*50ml": {
					"4": {
						"1": { "label": "Labware3", "labwareModelName": "Reagent Cooled 8*15ml" },
						"2": { "label": "buffer", "labwareModelName": "Reagent Cooled 8*50ml" },
						"internal": true
					}
				},
				"Trough 1000ml": {
					"7": {
						"1": { "label": "Wash buffer", "labwareModelName": "Trough 1000ml Std Vol Tips" },
						"internal": true
					}
				},
				"LI - Trough 3Pos 100mlOffset": {
					"8": { "internal": true }
				},
				"Downholder": {
					"9": { "internal": true }
				},
				"MP 2Pos H+P Shake": {
					"10": {
						"4": { "label": "Schuettelplatte", "labwareModelName": "DM Nunc stronghold" },
						"internal": true
					}
				},
				"Shelf 32Pos Microplate": {
					"11": {
						"1": { "labwareModelName": "DM Nunc stronghold" },
						"hotel": true,
						"external": { "n1": 0, "n2": 1 }
					}
				},
				"RoboPeel": {
					"12": {
						"external": { "n1": 0, "n2": 4 }
					}
				},
				"Block 20Pos": {
					"16": { "internal": true }
				},
				"MP 3Pos Cooled 1 PCR": {
					"17": {
						"2": { "label": "o/n culture", "labwareModelName": "DM Nunc stronghold" },
						"4": { "label": "Dilution MTP", "labwareModelName": "DM Nunc stronghold" },
						"internal": true
					}
				},
				"MP 3Pos Cooled 2 PCR": {
					"24": {
						"2": {
							"label": "MTP1",
							"labwareModelName": "DM Nunc stronghold"
						},
						"4": {
							"label": "MTP2",
							"labwareModelName": "DM Nunc stronghold"
						},
						"6": {
							"label": "MTP3",
							"labwareModelName": "DM Nunc stronghold"
						},
						"internal": true
					}
				},
				"Te-VacS": {
					"30": {
						"7": {
							"label": "Waste",
							"labwareModelName": "MTP Waste"
						},
						"internal": true
					}
				},
				"RoboSeal": {
					"35": {
						"external": {
							"n1": 0,
							"n2": 5
						}
					}
				},
				"TRobot1": {
					"40": {
						"internal": true,
						"external": {
							"n1": 0,
							"n2": 2
						}
					}
				},
				"TRobot2": {
					"47": {
						"internal": true,
						"external": {
							"n1": 0,
							"n2": 3
						}
					}
				},
				"Symbol954": {
					"49": {
						"1": {
							"labwareModelName": "DM Nunc stronghold"
						},
						"external": {
							"n1": 0,
							"n2": 0
						}
					}
				},
				"Centrifuge": {
					"54": {
						"external": {
							"n1": 0,
							"n2": 8
						}
					}
				},
				"Infinite M200": {
					"61": {
						"1": {
							"labwareModelName": "DM Nunc stronghold"
						},
						"external": {
							"n1": 0,
							"n2": 6
						}
					}
				},
				"ReGrip Station": {
					"59": {
						"1": {
							"label": "umgreifen",
							"labwareModelName": "DM Nunc stronghold"
						},
						"internal": true
					}
				},
				"Hotel 4Pos Transfer Grid 69": {
					"69": {
						"1": {
							"labwareModelName": "DM Nunc stronghold"
						},
						"hotel": true,
						"external": {
							"n1": 0,
							"n2": 7
						}
					}
				}
			});
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
			const s = EvowareTableFile.toStrings(carrierData, table1).join("\n")+"\n";
			//s = s.replace(/\r\n/g, "\n");
			//console.log(s);
			should.deepEqual(s, expected);
		});
	});
});
