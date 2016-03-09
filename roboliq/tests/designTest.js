const _ = require('lodash');
const should = require('should');
import {flattenDesign, printData} from '../src/design.js';

describe('design', () => {
	// Configure mathjs to use bignumbers
	require('mathjs').config({
		number: 'bignumber', // Default type of number
		precision: 64		// Number of significant digits for BigNumbers
	});
	describe('flattenDesign', () => {
		it('should handle two simple branching factors', () => {
			const design = {
				conditions: {
					"a*": [1, 2],
					"b*": [1, 2]
				}
			};
			const table = flattenDesign(design);
			should.deepEqual(table, [
				{a: 1, b: 1},
				{a: 1, b: 2},
				{a: 2, b: 1},
				{a: 2, b: 2},
			]);
		});

		it('should handle two factor levels with differing replicate counts', () => {
			const design = {
				conditions: {
					"treatment*": {
						"a": { },
						"b": {
							"*": { count: 2 }
						},
						"c": {
							"*": { count: 3 }
						}
					}
				}
			};
			const table = flattenDesign(design);
			//printData(table);
			should.deepEqual(table, [
				{treatment: "a"},
				{treatment: "b"},
				{treatment: "b"},
				{treatment: "c"},
				{treatment: "c"},
				{treatment: "c"},
			]);
		});

		it('should handle conditions nested inside branching values', () => {
			const design = {
				conditions: {
					"treatment*": {
						"A": { "*": { count: 2 } },
						"B": { "*": { count: 2 } },
					},
					"order=range": {},
				},
				models: {
					model1: {
						treatmentFactors: ["treatment"],
						experimentalFactors: ["batch"],
						samplingFactors: ["batch"],
						measurementFactors: ["yield"],
						orderFactors: ["batch"],
						formula: "yield ~ treatment",
						assumptions: {
							sameVariance: true
						}
					}
				}
			};
			const table = flattenDesign(design);
			//printData(table);
			should.deepEqual(table, [
				{treatment: "A", order: 1},
				{treatment: "A", order: 2},
				{treatment: "B", order: 3},
				{treatment: "B", order: 4},
			]);
		});

		it("should handle a branching array of objects", () => {
			const design = {
				conditions: {
					"media": "media1",
					"culturePlate*": {
						"stillPlate": {
							"cultureReplicate*": [
								{
									"cultureWell": "A01",
									"measurement*": [
										{"dilutionPlate": "dilutionPlate1"}
									]
								}
							]
						}
					}
				}
			}
			const table = flattenDesign(design);
			//printData(table);
			should.deepEqual(table, [
				{media: "media1", culturePlate: "stillPlate", cultureReplicate: 1, cultureWell: "A01", measurement: 1, dilutionPlate: "dilutionPlate1"}
			]);
		});

		it.skip('should produce factors for Box dataset Chapter 3, boys shoes', () => {
			const design = {
				randomSeed: 5,
				conditionsWorks: {
					"boy*=range": {till: 4},
					"left=": {values: ["A", "B"], sample: true},
					"material*": ["A", "B"],
					"foot=math": "(left == material) ? \"left\" : \"right\""
				},
				conditions: {
					"boy*=range": {till: 4},
					"condition=sample": [
						[{material: "A", foot: "L"}, {material: "B", foot: "R"}],
						[{material: "A", foot: "R"}, {material: "B", foot: "L"}]
					]
				},
				conditionsWorkedSortOf1: {
					"boy*=range": {till: 4},
					"condition=": {
						sample: true,
						values: [
							{
								"material*": {
									"A": {foot: "L"},
									"B": {foot: "R"}
								}
							},
							{
								"material*": {
									"A": {foot: "R"},
									"B": {foot: "L"}
								}
							},
						]
					}
				},/*
				conditions0: {
					"boy*=range": {till: 2},
					"material*": ["A", "B"],
					"foot=assign": {
						values: ["L", "R", "R", "L"],
						rotateValues: true
					}
				},
				conditions4: {
					"left*": ["A", "B"],
					"replicate*": [1,2,3,4,5],
					"boy=range": {
						shuffle: true
					},
					"material*": ["A", "B"],
					"foot=math": "(left == material) ? \"left\" : \"right\""
				},
				consitions3: {
					"materialA*": {
						"L": {
							"material*": ["A", "B"],
							"foot=assign": ["L", "R"]
						},
						"R": {
							"material*": ["A", "B"],
							"foot=assign": ["R", "L"]
						}
					}
				},
				conditions2: {
					"material": "A",
					"boy*=range": {till: 10},
					"foot=assign": {
						values: ["L", "R"],
						shuffle: true,
						rotateValues: true
					},
					"*": {
						conditions: {
							"material": "B",
							"foot=math": '(foot == "L") ? "R" : "L"'
						}
					}
				},
				conditions1: {
					"boy*=range": {till: 2},
					"foot*": ["L", "R"],
					"material=assign": {
						values: ["A", "B"],
						groupBy: "boy",
						shuffle: true
					}
				},
				models: {
					model1: {
						treatmentFactors: ["material"],
						experimentalFactors: ["foot"],
						samplingFactors: ["foot"],
						measurementFactors: ["wear"],
						formula: "wear ~ material",
					}
				}*/
			};
			const table = flattenDesign(design);
			console.log(JSON.stringify(table, null, '\t'))
			printData(table);
			should.deepEqual(table, [
				{material: "A", boy: 1, foot: 1},
				{treatment: "A", order: 2},
				{treatment: "B", order: 3},
				{treatment: "B", order: 4},
			]);
		});

		it("should keep factors in the correct order", () => {
			const design =
				{ conditions:
					{ waterSource: 'saltwater',
						waterVolume: '40ul',
						'proteinSource*': [ 'sfGFP', 'Q204H_N149Y', 'tdGFP', 'N149Y', 'Q204H' ],
						proteinVolume: '5ul',
						'bufferSystem*':
						 { acetate:
								{ acidSource: 'acetate_375',
									baseSource: 'acetate_575',
									acidPH: 3.75,
									basePH: 5.75 } } } };
			const table = flattenDesign(design);
			//printData(table);
			should.deepEqual(_.keys(table[0]),
				["waterSource", "waterVolume", "proteinSource", "proteinVolume", "bufferSystem", "acidSource", "baseSource", "acidPH", "basePH"]
			);
		});

		it("should support numbers with units in range() assignment", () => {
			const design =
				{ conditions:
					{ source: "water",
						'volume*=range': { count: 4, from: 0, till: 20, decimals: 1, units: "ul" } } };
			const table = flattenDesign(design);
			//printData(table);
			should.deepEqual(table, [
				{source: "water", volume: "0 ul"},
				{source: "water", volume: "6.7 ul"},
				{source: "water", volume: "13.3 ul"},
				{source: "water", volume: "20 ul"}
			]);
		});

		it("should support math() assignments", () => {
			const design =
				{ conditions:
					{ volume1: "20ul",
						"volume2=math": "30ul - volume1" } };
			const table = flattenDesign(design);
			//printData(table);
			should.deepEqual(table, [
				{volume1: "20ul", volume2: "10 ul"}
			]);
		});

		it("should support math() assignments with parameters", () => {
			const design = {
				conditions: {
					source: 'saltwater',
					acidPH: 3.75,
					basePH: 5.75,
					'acidVolume*=range': { count: 4, from: 0, till: 20, decimals: 1, units: 'ul' },
					'baseVolume=math': {
						expression: '20ul - acidVolume',
						decimals: 3,
						units: "ml"
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printData(table);
			should.deepEqual(table, [
				{"source":"saltwater","acidPH":3.75,"basePH":5.75,"acidVolume":"0 ul","baseVolume":"0.020 ml"},
				{"source":"saltwater","acidPH":3.75,"basePH":5.75,"acidVolume":"6.7 ul","baseVolume":"0.013 ml"},
				{"source":"saltwater","acidPH":3.75,"basePH":5.75,"acidVolume":"13.3 ul","baseVolume":"0.007 ml"},
				{"source":"saltwater","acidPH":3.75,"basePH":5.75,"acidVolume":"20 ul","baseVolume":"0.000 ml"}
			]);
		});

		it("should support allocateWells() assignments", () => {
			const design1 = {
				conditions: {
					"replicate*=range": {till: 5},
					"well=allocateWells": {rows: 2, columns: 3}
				}
			};
			const table1 = flattenDesign(design1);
			// console.log(JSON.stringify(table1))
			// printData(table1);
			should.deepEqual(table1, [
				{"replicate":1,"well":"A01"},
				{"replicate":2,"well":"B01"},
				{"replicate":3,"well":"A02"},
				{"replicate":4,"well":"B02"},
				{"replicate":5,"well":"A03"}
			]);

			const design2 = {
				conditions: {
					"replicate*=range": {till: 5},
					"well=allocateWells": {rows: 2, columns: 3, byColumns: false}
				}
			};
			const table2 = flattenDesign(design2);
			// console.log(JSON.stringify(table1))
			// printData(table1);
			should.deepEqual(table2, [
				{"replicate":1,"well":"A01"},
				{"replicate":2,"well":"A02"},
				{"replicate":3,"well":"A03"},
				{"replicate":4,"well":"B01"},
				{"replicate":5,"well":"B02"}
			]);
		});

		it("should support range() with groupBy", () => {
			const design = {
				conditions: {
					"a*": [1, 2],
					"b*": [1, 2, 3],
					"order=range": {
						groupBy: "a"
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printData(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3},
				{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}
			]);
		});

		it("should support range() with sameBy", () => {
			const design = {
				conditions: {
					"a*": [1, 2],
					"b*": [1, 2, 3],
					"order=range": {
						sameBy: "a"
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printData(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":1},{"a":1,"b":3,"order":1},
				{"a":2,"b":1,"order":2},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":2}
			]);
		});

		it("should support range() with groupBy and shuffle", () => {
			const design = {
				randomSeed: 444,
				conditions: {
					"a*": [1, 2],
					"b*": [1, 2, 3],
					"order=range": {
						groupBy: "a",
						shuffle: true
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printData(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"order":2},{"a":1,"b":2,"order":1},{"a":1,"b":3,"order":3},
				{"a":2,"b":1,"order":2},{"a":2,"b":2,"order":3},{"a":2,"b":3,"order":1}
			]);
		});

		it("should support range() with groupBy and shuffle and shuffleOnce", () => {
			const design = {
				randomSeed: 444,
				conditions: {
					"a*": [1, 2],
					"b*": [1, 2, 3],
					"order=range": {
						till: 3,
						groupBy: "a",
						shuffle: true,
						shuffleOnce: true
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printData(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"order":2},{"a":1,"b":2,"order":1},{"a":1,"b":3,"order":3},
				{"a":2,"b":1,"order":2},{"a":2,"b":2,"order":1},{"a":2,"b":3,"order":3}
			]);
		});

		it("should support assignment of an array of objects", () => {
			const design = {
				conditions: {
					"a*": [1, 2],
					"b=": [{c: 1}, {c: 2}]
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printData(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"c":1}, {"a":2,"b":2,"c":2}
			]);
		});

		it("should support assignment of a nested array of objects, implicitly resulting in branching", () => {
			const design = {
				conditions: {
					"a*": [1, 2],
					"b=": [[{c: 1}, {c: 2}], [{c: 3}, {c: 4}]]
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printData(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"c":1}, {"a":1,"b":1,"c":2},
				{"a":2,"b":2,"c":3}, {"a":2,"b":2,"c":4},
			]);
		});

		it.skip("should support sampling assignment of a nested array of objects, implicitly resulting in branching", () => {
			const design = {
				conditions: {
					"a*": [1, 2, 3, 4],
					"b=sample": [[{c: 1}, {c: 2}], [{c: 3}, {c: 4}]]
				}
			};
			const table = flattenDesign(design);
			console.log(JSON.stringify(table))
			printData(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"c":1}, {"a":1,"b":1,"c":2},
				{"a":2,"b":2,"c":3}, {"a":2,"b":2,"c":4},
				{"a":3,"b":1,"c":1}, {"a":3,"b":1,"c":2},
				{"a":4,"b":2,"c":3}, {"a":4,"b":2,"c":4},
			]);
		});

	});
});
