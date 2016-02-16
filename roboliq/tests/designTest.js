const _ = require('lodash');
const should = require('should');
import {flattenDesign} from '../src/design.js';

describe('design', () => {
	describe('flattenDesign', () => {
		it('renders a pair of buttons', () => {
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
	});
});
