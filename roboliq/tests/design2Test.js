const _ = require('lodash');
const should = require('should');
import {printData} from '../src/design.js';
import {flattenArrayM, expandConditions} from '../src/design2.js';

describe('design', () => {
	// Configure mathjs to use bignumbers
	require('mathjs').config({
		number: 'bignumber', // Default type of number
		precision: 64		// Number of significant digits for BigNumbers
	});

	describe("flattenArrayM", () => {
		const l0 = [
			{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3},
			{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}
		];
		it('should handle an already-flat array', () => {
			should.deepEqual(flattenArrayM(_.cloneDeep(l0)), l0);
		});

		it('should handle one level of nesting', () => {
			const l = [
				[{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3}],
				[{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}]
			];
			should.deepEqual(flattenArrayM(_.cloneDeep(l)), l0);
		});

		it('should handle two levels of nesting', () => {
			const l = [
				[{"a":1,"b":1,"order":1},[{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3}]],
				[[{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}]]
			];
			should.deepEqual(flattenArrayM(_.cloneDeep(l)), l0);
		});
	});

	describe.only("expandConditions", () => {
		it("should 1", () => {
			should.deepEqual(
				expandConditions({
					"a": 1
				}),
				[
					{a: 1}
				]
			);
		})
	});
});
