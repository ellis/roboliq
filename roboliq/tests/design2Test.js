const _ = require('lodash');
const should = require('should');
import {printData} from '../src/design.js';
import {flattenDesign, flattenArrayM, expandConditions} from '../src/design2.js';

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

		it("should handle assignment of two branching arrays", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1,2],
					"b*": [1, 2, 3]
				}),
				[
					{a: 1, b: 1}, {a: 1, b: 2}, {a: 1, b: 3},
					{a: 2, b: 1}, {a: 2, b: 2}, {a: 2, b: 3}
				]
			);
		});

		it("should handle 'range' action", () => {
			should.deepEqual(
				expandConditions({
					"a*=range": {till: 4},
					"b=range": {from: 0, till: 3},
					"c=range": {from: 10, till: 100, step: 10},
					"d=range": {}
				}),
				[
					{a: 1, b: 0, c: 10, d: 1},
					{a: 2, b: 1, c: 20, d: 2},
					{a: 3, b: 2, c: 30, d: 3},
					{a: 4, b: 3, c: 40, d: 4}
				]
			);
		});

		it("should handle assign() with order=restart", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1, 2, 3, 4],
					"b=": {
						values: [1, 2],
						order: "restart"
					}
				}),
				[
					{a: 1, b: 1}, {a: 2, b: 2},
					{a: 3, b: 1}, {a: 4, b: 2}
				]
			);
		});

		it("should handle assign() with order=reverse", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1, 2, 3, 4],
					"b=": {
						values: [1, 2],
						order: "reverse"
					}
				}),
				[
					{a: 1, b: 1}, {a: 2, b: 2},
					{a: 3, b: 2}, {a: 4, b: 1}
				]
			);
		});

		it("should handle assign() with order=reshuffle", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1, 2, 3, 4, 5, 6],
					"b=": {
						randomSeed: 444,
						values: [1, 2, 3],
						order: "reshuffle"
					}
				}),
				[
					{a: 1, b: 2}, {a: 2, b: 1}, {a: 3, b: 3},
					{a: 4, b: 2}, {a: 5, b: 3}, {a: 6, b: 1}
				]
			);
		});

	});

	describe("flattenDesign", () => {

		it("should handle assignment of two branching arrays", () => {
			should.deepEqual(
				flattenDesign({
					conditions: {
						"a*": [1,2],
						"b*": [1, 2, 3]
					}
				}),
				[
					{a: 1, b: 1}, {a: 1, b: 2}, {a: 1, b: 3},
					{a: 2, b: 1}, {a: 2, b: 2}, {a: 2, b: 3}
				]
			);
		});

		it("should handle assign() with order=shuffle", () => {
			const design = {
				randomSeed: 444,
				conditions: {
					"a*": [1, 2],
					"b*": [1, 2, 3],
					"order=": {
						values: [1, 2, 3, 4, 5, 6],
						order: "shuffle"
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printData(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":5},{"a":1,"b":3,"order":3},
				{"a":2,"b":1,"order":4},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":6}
			]);
		});

	});
});
