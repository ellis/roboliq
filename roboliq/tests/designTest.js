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
	});
});
