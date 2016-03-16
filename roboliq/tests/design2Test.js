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

	describe("expandConditions", () => {
		it("should handle a simple value", () => {
			should.deepEqual(
				expandConditions({
					"a": 1
				}),
				[
					{a: 1}
				]
			);
		});

		it("should handle a simple branching array", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1,2,3]
				}),
				[
					{a: 1}, {a: 2}, {a: 3}
				]
			);
		});

		it("should handle a simple branching object", () => {
			should.deepEqual(
				expandConditions({
					"a*": {
						"A": {b: 1},
						"B": {b: 2},
						"C": {b: 3},
					}
				}),
				[
					{a: "A", b: 1}, {a: "B", b: 2}, {a: "C", b: 3}
				]
			);
		});

		it("should handle a branching array of simple objects", () => {
			should.deepEqual(
				expandConditions({
					"a*": [
						{b: 1},
						{b: 2},
						{b: 3},
					]
				}),
				[
					{a: 1, b: 1}, {a: 2, b: 2}, {a: 3, b: 3}
				]
			);
		});

		it("should handle assignment of an array", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1,2,3],
					"b": [4,5,6]
				}),
				[
					{a: 1, b: 4}, {a: 2, b: 5}, {a: 3, b: 6}
				]
			);
		});

		it("should handle assignment of an object", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1,2,3],
					"b": {a: {c: "q"}, b: {c: "r"}, c: {c: "s"}}
				}),
				[
					{a: 1, b: "a", c: "q"}, {a: 2, b: "b", c: "r"}, {a: 3, b: "c", c: "s"}
				]
			);
		});

		it("should handle assignment of an array of arrays", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1,2],
					"b": [
						[{c: "p"}, {c: "q"}],
						[{c: "r"}, {c: "s"}]
					]
				}),
				[
					{a: 1, b: 1, c: "p"}, {a: 1, b: 1, c: "q"},
					{a: 2, b: 2, c: "r"}, {a: 2, b: 2, c: "s"}
				]
			);
		});

		it("should handle assignment of an object of arrays", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1,2],
					"b": {a: [{c: "p"}, {c: "q"}], b: [{c: "r"}, {c: "s"}]}
				}),
				[
					{a: 1, b: "a", c: "p"}, {a: 1, b: "a", c: "q"},
					{a: 2, b: "b", c: "r"}, {a: 2, b: "b", c: "s"}
				]
			);
		});

	});
});
