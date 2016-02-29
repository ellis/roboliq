const _ = require('lodash');
const should = require('should');
import {flattenDesign, printData} from '../src/design.js';

describe('design', () => {
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
				conditions: {
					"boy*=range": {till: 2},
					"material*": ["A", "B"],
					"foot=assign": {
						values: ["L", "R", "R", "L"],
						rotateValues: true
					}
				},
				conditions2: {
					"material": "A",
					"boy*=range": {till: 10},
					"foot=assign": {
						values: ["L", "R"],
						random: true,
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
						random: true
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
				}
			};
			const table = flattenDesign(design);
			printData(table);
			should.deepEqual(table, [
				{material: "A", boy: 1, foot: 1},
				{treatment: "A", order: 2},
				{treatment: "B", order: 3},
				{treatment: "B", order: 4},
			]);
		});

	});
});
