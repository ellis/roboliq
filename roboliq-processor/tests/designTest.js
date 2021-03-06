/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

const _ = require('lodash');
const should = require('should');
const {flattenDesign, flattenArrayM, flattenArrayAndIndexes, query_groupBy, expandConditions, printRows} = require('../src/design.js');

describe('design', () => {
	// Configure mathjs to use bignumbers
	require('mathjs').config({
		number: 'BigNumber', // Default type of number
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

	describe("flattenArrayAndIndexes", () => {
		const l0 = [
			{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3},
			{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}
		];
		it('should handle an already-flat array', () => {
			const rows = _.cloneDeep(l0);
			const rowIndexes = _.range(rows.length);
			flattenArrayAndIndexes(rows, rowIndexes);
			should.deepEqual(rows, l0);
			should.deepEqual(rowIndexes, _.range(6));
		});

		it('should handle one level of nesting', () => {
			const rows = [
				[{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3}],
				[{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}]
			];
			const rowIndexes = _.range(rows.length);
			flattenArrayAndIndexes(rows, rowIndexes);
			// console.log(JSON.stringify(rowIndexes))
			// console.log(JSON.stringify(rows, null, '\t'))
			should.deepEqual(rows, l0);
			should.deepEqual(rowIndexes, _.range(6));
		});

		it('should handle partial flattening #1', () => {
			const rows = [
				[{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3}],
				[{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}]
			];
			const rowIndexes = [0];
			flattenArrayAndIndexes(rows, rowIndexes);
			should.deepEqual(rows, [
				{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3},
				[{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}]
			]);
			should.deepEqual(rowIndexes, [0, 1, 2]);
		});

		it('should handle partial flattening #2', () => {
			const rows = [
				[{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3}],
				[{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}]
			];
			const rowIndexes = [1];
			flattenArrayAndIndexes(rows, rowIndexes);
			// console.log(JSON.stringify(rowIndexes))
			// console.log(JSON.stringify(rows, null, '\t'))
			should.deepEqual(rows, [
				[{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3}],
				{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}
			]);
			should.deepEqual(rowIndexes, [1, 2, 3]);
		});

		it('should handle two levels of nesting', () => {
			const rows = [
				[{"a":1,"b":1,"order":1},[{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3}]],
				[[{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}]]
			];
			const rowIndexes = _.range(rows.length);
			flattenArrayAndIndexes(rows, rowIndexes);
			should.deepEqual(rows, l0);
			should.deepEqual(rowIndexes, _.range(6));
		});

		it("should handle a previous bug", () => {
			const rowsExpected = [{"a":"0","b":1},{"a":"0","b":2}];
			const rows = [rowsExpected];
			const rowIndexes = [0];
			const otherRowIndexes = [[0],[0],[0],[0]];
			flattenArrayAndIndexes(rows, rowIndexes, otherRowIndexes);
			should.deepEqual(rows, rowsExpected);
			should.deepEqual(rowIndexes, _.range(2));
			should.deepEqual(otherRowIndexes, [[0, 1], [0, 1], [0, 1], [0, 1]]);
		});
	});

	describe("query_groupBy", () => {
		it("should handle grouping of rowIndexes in flattened rows array", () => {
			const rows = [{a: 1, b: 1, c: 1}, {a: 1, b: 1, c: 2}, {a: 1, b: 2, c: 3}];
			should.deepEqual(
				query_groupBy(rows, _.range(rows.length), "a"),
				[[0, 1, 2]]
			);
			should.deepEqual(
				query_groupBy(rows, _.range(rows.length), "b"),
				[[0, 1], [2]]
			);
			should.deepEqual(
				query_groupBy(rows, _.range(rows.length), "c"),
				[[0], [1], [2]]
			);
			should.deepEqual(
				query_groupBy(rows, _.range(rows.length), ["a", "b"]),
				[[0, 1], [2]]
			);
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

		it("should handle a branching array of branching arrays", () => {
			should.deepEqual(
				expandConditions({
					"a*": [
						{"b*": 2},
						{"c*": 2},
					]
				}),
				[
					{a: 1, b: 1}, {a: 1, b: 2},
					{a: 2, c: 1}, {a: 2, c: 2}
				]
			);
		});

		it("should handle a branching array of complex objects", () => {
			should.deepEqual(
				expandConditions({
					"a*": 2,
					".b": [
					  {"c*": [1], d: [4]},
						{"c*": [1, 2], d: [0, 4]}
					]
				}),
				[
					{a: 1, c: 1, d: 4},
					{a: 2, c: 1, d: 0},
					{a: 2, c: 2, d: 4}
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

		it("should handle branching factors whose value is a number, and without a name", () => {
			should.deepEqual(
				expandConditions({
					"a*": 2,
					"*": 2
				}),
				[
					{a: 1}, {a: 1},
					{a: 2}, {a: 2}
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

		it("should handle 'range' action with expressions for property values", () => {
			should.deepEqual(
				expandConditions({
					"a*=range": {till: 3},
					"b": 2,
					"c=range": {from: "b", till: 4}
				}),
				[
					{a: 1, b: 2, c: 2},
					{a: 2, b: 2, c: 3},
					{a: 3, b: 2, c: 4},
				]
			);
		});

		it("should handle branching 'range' action with expressions for property values", () => {
			should.deepEqual(
				expandConditions({
					"a*=range": {till: 3},
					"b*=range": {till: "a", groupBy: "a"}
				}),
				[
					{a: 1, b: 1},
					{a: 2, b: 1},
					{a: 2, b: 2},
					{a: 3, b: 1},
					{a: 3, b: 2},
					{a: 3, b: 3},
				]
			);
		});

		it("should handle 'rotateColumn' action", () => {
			should.deepEqual(
				expandConditions({
					"a*": 4,
					"b=rotateColumn": "a",
					"c=rotateColumn": { column: "a", n: 2 },
					"d=rotateColumn": { column: "a", n: -1 }
				}),
				[
					{a: 1, b: 4, c: 3, d: 2},
					{a: 2, b: 1, c: 4, d: 3},
					{a: 3, b: 2, c: 1, d: 4},
					{a: 4, b: 3, c: 2, d: 1},
				]
			);
		});

		it("should handle assign() with order=repeat", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1, 2, 3, 4],
					"b=": {
						values: [1, 2],
						order: "repeat"
					}
				}),
				[
					{a: 1, b: 1}, {a: 2, b: 2},
					{a: 3, b: 1}, {a: 4, b: 2}
				]
			);
		});

		it("should handle assign() with order=repeat #2", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1, 2, 3, 1, 2, 3],
					"c*": [{
						"b=": {
							values: [1, 2, 3],
							order: "repeat",
						}
					}]
				}),
				[
					{a: 1, c: 1, b: 1}, {a: 2, c: 1, b: 2}, {a: 3, c: 1, b: 3},
					{a: 1, c: 1, b: 1}, {a: 2, c: 1, b: 2}, {a: 3, c: 1, b: 3}
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
						randomSeed: 1,
						values: [1, 2, 3],
						order: "reshuffle"
					}
				}),
				[
					{a: 1, b: 1}, {a: 2, b: 3}, {a: 3, b: 2},
					{a: 4, b: 2}, {a: 5, b: 3}, {a: 6, b: 1}
				]
			);
		});

		it("should handle assign() with order=repeat and orderBy", () => {
			should.deepEqual(
				expandConditions({
					"a*": [6, 5, 4, 3, 2, 1],
					"b=": {
						values: [1, 2, 3],
						order: "repeat",
						orderBy: "a"
					}
				}),
				[
					{a: 6, b: 3}, {a: 5, b: 2}, {a: 4, b: 1},
					{a: 3, b: 3}, {a: 2, b: 2}, {a: 1, b: 1}
				]
			);
		});

		it("should handle assign() with order=repeat and orderBy #2", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1, 2, 3, 1, 2, 3],
					"b=": {
						values: [1, 2, 3],
						order: "repeat",
						orderBy: "a"
					}
				}),
				[
					{a: 1, b: 1}, {a: 2, b: 3}, {a: 3, b: 2},
					{a: 1, b: 2}, {a: 2, b: 1}, {a: 3, b: 3}
				]
			);
		});

		it.skip("should handle assign() with order=repeat and orderBy #3", () => {
			// The problem is that branchRowsByNamedValue goes through one row at a time, rather than assigning to all rows that get branched
			should.deepEqual(
				expandConditions({
					"a*": [1, 2, 3, 1, 2, 3],
					"c*": [{
						"b=": {
							values: [1, 2, 3],
							order: "repeat",
							orderBy: "a"
						}
					}]
				}),
				[
					{a: 1, c: 1, b: 1}, {a: 2, c: 1, b: 3}, {a: 3, c: 1, b: 2},
					{a: 1, c: 1, b: 2}, {a: 2, c: 1, b: 1}, {a: 3, c: 1, b: 3}
				]
			);
		});

		it("should handle assign() with groupBy", () => {
			should.deepEqual(
				expandConditions({
					"a*": [1, 2],
					"b*": [1,2,3],
					"c=": {
						groupBy: "a",
						values: [4, 5, 6]
					}
				}),
				[
					{a: 1, b: 1, c: 4}, {a: 1, b: 2, c: 5}, {a: 1, b: 3, c: 6},
					{a: 2, b: 1, c: 4}, {a: 2, b: 2, c: 5}, {a: 2, b: 3, c: 6}
				]
			);
		});

		it("should handle assign() with orderBy", () => {
			should.deepEqual(
				expandConditions({
					"a*": [3, 1, 2, 4],
					"c=": {
						orderBy: "a",
						values: [10, 20, 30, 40]
					}
				}),
				[
					{a: 3, c: 30},
					{a: 1, c: 10},
					{a: 2, c: 20},
					{a: 4, c: 40}
				]
			);
		});

		it("should handle calculate()", () => {
			should.deepEqual(
				expandConditions({
					"a*": 2,
					"b=calculate": "a * 2"
				}),
				[
					{a: 1, b: 2}, {a: 2, b: 4}
				]
			);
		});

		it("should handle assign() with calculate value", () => {
			should.deepEqual(
				expandConditions({
					"a*": 2,
					"b=": {
						calculate: "a * 2"
					}
				}),
				[
					{a: 1, b: 2}, {a: 2, b: 4}
				]
			);
		});

		it("should handle branching range() with custom 'from' value #1", () => {
			should.deepEqual(
				expandConditions({
					"a*=range": {from: 0, till: 3}
				}),
				[
					{a: 0}, {a: 1}, {a: 2}, {a: 3}
				]
			);
		});

		it("should handle branching range() with custom 'from' value #2", () => {
			should.deepEqual(
				expandConditions({
					"a*=range": {from: 0, till: 3},
					"b*=range": {from: 0, till: 1}
				}),
				[
					{a: 0, b: 0}, {a: 0, b: 1},
					{a: 1, b: 0}, {a: 1, b: 1},
					{a: 2, b: 0}, {a: 2, b: 1},
					{a: 3, b: 0}, {a: 3, b: 1},
				]
			);
		});

		it("should handle case()", () => {
			/*const rows = [{"a":1,"b":"A"},{"a":2},{"a":3}];
			const rowIndexes = [0];
			const otherRowIndexes = [[0,1,2],[0],[0,1,2]];
			expandRowsByNamedValue(rows, rowIndexes, otherRowIndexes, "c*", 2, undefined);
			should.deepEqual(otherRowIndexes, [[0,1,2,3], [0,1], [0,1,2,3]])*/

			should.deepEqual(
				expandConditions({
					"a*": 3,
					"b=case": {
						cases: {
							A: {
								where: {a: 1},
								design: {
									"c*": 2
								}
							},
							B: {}
						}
					}
				}),
				[
					{a: 1, b: "A", c: 1}, {a: 1, b: "A", c: 2},
					{a: 2, b: "B"},
					{a: 3, b: "B"}
				]
			);
		});

		it("should handle case() #2", () => {
			/*const rows = [{"a":1,"b":"A"},{"a":2},{"a":3}];
			const rowIndexes = [0];
			const otherRowIndexes = [[0,1,2],[0],[0,1,2]];
			expandRowsByNamedValue(rows, rowIndexes, otherRowIndexes, "c*", 2, undefined);
			should.deepEqual(otherRowIndexes, [[0,1,2,3], [0,1], [0,1,2,3]])*/

			should.deepEqual(
				expandConditions({
					"a=case": [
						{
							design: {
								"b*": 2,
								"c": 0
							}
						}
					]
				}),
				[
					{a: 1, b: 1, c: 0},
					{a: 1, b: 2, c: 0}
				]
			);
		});

		it("should handle calculateWell()", () => {
			should.deepEqual(
				expandConditions({
					"i*": [1,2,3],
					"j*": [1,2,3],
					"well=calculateWell": {row: "i+1", column: "j*2"}
				}),
				[
					{i: 1, j: 1, well: "B02"}, {i: 1, j: 2, well: "B04"}, {i: 1, j: 3, well: "B06"},
					{i: 2, j: 1, well: "C02"}, {i: 2, j: 2, well: "C04"}, {i: 2, j: 3, well: "C06"},
					{i: 3, j: 1, well: "D02"}, {i: 3, j: 2, well: "D04"}, {i: 3, j: 3, well: "D06"}
				]
			);
		});

		it("should handle concat() without groupBy", () => {
			should.deepEqual(
				expandConditions({
					"i*": [1,2,3],
					".=concat": {
						design: {
							j: 1
						}
					}
				}),
				[
					{i: 1}, {i: 2}, {i: 3}, {j: 1}
				]
			)
		});

		it("should handle concat() with groupBy", () => {
			should.deepEqual(
				expandConditions({
					"i*": 2,
					"j*": 2,
					".=concat": {
						groupBy: "i",
						design: {
							"k=calculate": "i * 2"
						}
					}
				}),
				[
					{i: 1, j: 1}, {i: 1, j: 2}, {i: 2, j: 1}, {i: 2, j: 2},
					{i: 1, k: 2}, {i: 2, k: 4}
				]
			)
		});

		it("should support allocateWells() with sameBy", () => {
			should.deepEqual(
				expandConditions({
					"a*": 2,
					"b*": 2,
					".c*": [
						{
							"d": 1,
							"e*": [ 4 ],
							"f=range": {}
						},
						{
							"d": 2,
							"e*": [ 0, 3 ],
							"f=range": {}
						}
					],
					"g*": 2,
					"w=allocateWells": {
						"rows": 8, "columns": 12,
						"sameBy": ["a", "b", "d"]
					}
				}),
				[
					{ "a": 1, "b": 1, "d": 1, "e": 4, "f": 1, "g": 1, "w": "A01" },
					{ "a": 1, "b": 1, "d": 1, "e": 4, "f": 1, "g": 2, "w": "A01" },
					{ "a": 1, "b": 1, "d": 2, "e": 0, "f": 1, "g": 1, "w": "B01" },
					{ "a": 1, "b": 1, "d": 2, "e": 0, "f": 1, "g": 2, "w": "B01" },
					{ "a": 1, "b": 1, "d": 2, "e": 3, "f": 2, "g": 1, "w": "B01" },
					{ "a": 1, "b": 1, "d": 2, "e": 3, "f": 2, "g": 2, "w": "B01" },
					{ "a": 1, "b": 2, "d": 1, "e": 4, "f": 2, "g": 1, "w": "C01" },
					{ "a": 1, "b": 2, "d": 1, "e": 4, "f": 2, "g": 2, "w": "C01" },
					{ "a": 1, "b": 2, "d": 2, "e": 0, "f": 3, "g": 1, "w": "D01" },
					{ "a": 1, "b": 2, "d": 2, "e": 0, "f": 3, "g": 2, "w": "D01" },
					{ "a": 1, "b": 2, "d": 2, "e": 3, "f": 4, "g": 1, "w": "D01" },
					{ "a": 1, "b": 2, "d": 2, "e": 3, "f": 4, "g": 2, "w": "D01" },
					{ "a": 2, "b": 1, "d": 1, "e": 4, "f": 3, "g": 1, "w": "E01" },
					{ "a": 2, "b": 1, "d": 1, "e": 4, "f": 3, "g": 2, "w": "E01" },
					{ "a": 2, "b": 1, "d": 2, "e": 0, "f": 5, "g": 1, "w": "F01" },
					{ "a": 2, "b": 1, "d": 2, "e": 0, "f": 5, "g": 2, "w": "F01" },
					{ "a": 2, "b": 1, "d": 2, "e": 3, "f": 6, "g": 1, "w": "F01" },
					{ "a": 2, "b": 1, "d": 2, "e": 3, "f": 6, "g": 2, "w": "F01" },
					{ "a": 2, "b": 2, "d": 1, "e": 4, "f": 4, "g": 1, "w": "G01" },
					{ "a": 2, "b": 2, "d": 1, "e": 4, "f": 4, "g": 2, "w": "G01" },
					{ "a": 2, "b": 2, "d": 2, "e": 0, "f": 7, "g": 1, "w": "H01" },
					{ "a": 2, "b": 2, "d": 2, "e": 0, "f": 7, "g": 2, "w": "H01" },
					{ "a": 2, "b": 2, "d": 2, "e": 3, "f": 8, "g": 1, "w": "H01" },
					{ "a": 2, "b": 2, "d": 2, "e": 3, "f": 8, "g": 2, "w": "H01" }
				]
			);
		});

		it("should support allocateWells() with sameBy and order=shuffle", () => {
			should.deepEqual(
				expandConditions({
					"a*": 2,
					"b*": 2,
					"w=allocateWells": {
						"rows": 8, "columns": 12,
						"sameBy": ["a"],
						"order": "shuffle",
						"randomSeed": 100
					}
				}),
				[
					{a: 1, b: 1, w: "E03"},
					{a: 1, b: 2, w: "E03"},
					{a: 2, b: 1, w: "E12"},
					{a: 2, b: 2, w: "E12"},
				]
			);
		});

		it("should support allocateWells() with orderBy on a numeric field", () => {
			should.deepEqual(
				expandConditions({
					"step*": [1, 2, 10],
					"well=allocateWells": {
						orderBy: "step",
						rows: 8,
						columns: 12,
						wells: "A01 right A12"
					}
				}),
				[
					{step: 1, well: "A01"},
					{step: 2, well: "A02"},
					{step: 10, well: "A03"},
				]
			);
		});

		it("should support allocateWells() with `from` property", () => {
			should.deepEqual(
				expandConditions({
					"step*": 4,
					"well=allocateWells": {
						rows: 8,
						columns: 12,
						from: "G02"
					}
				}),
				[
					{step: 1, well: "G02"},
					{step: 2, well: "H02"},
					{step: 3, well: "A03"},
					{step: 4, well: "B03"},
				]
			);
		});

		it("should support range() with groupBy and sameBy", () => {
			should.deepEqual(
				expandConditions({
					"a*": 2,
					"b*": 2,
					".replicate*": 2,
					"c=range": {
						groupBy: "a"
					},
					"d=range": {
						"groupBy": ["a"],
						"sameBy": ["b"]
					}
				}),
				[
					{a: 1, b: 1, c: 1, d: 1},
					{a: 1, b: 1, c: 2, d: 1},
					{a: 1, b: 2, c: 3, d: 2},
					{a: 1, b: 2, c: 4, d: 2},
					{a: 2, b: 1, c: 1, d: 1},
					{a: 2, b: 1, c: 2, d: 1},
					{a: 2, b: 2, c: 3, d: 2},
					{a: 2, b: 2, c: 4, d: 2},
				]
			);
		});

		it("should support a list of conditions which will be aggregated", () => {
			should.deepEqual(
				expandConditions([
					{
						"a*": 2,
						"b*": 2
					},
					{
						"c*": 4
					}
				]),
				[
					{a: 1, b: 1, c: 1},
					{a: 1, b: 2, c: 2},
					{a: 2, b: 1, c: 3},
					{a: 2, b: 2, c: 4},
				]
			);
		});

	});

	describe("flattenDesign", () => {

		it("should handle assignment of two branching arrays", () => {
			should.deepEqual(
				flattenDesign({
					design: {
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
				design: {
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
			// printRows(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":5},{"a":1,"b":3,"order":3},
				{"a":2,"b":1,"order":4},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":6}
			]);
		});

		it('should handle two simple branching factors', () => {
			const design = {
				design: {
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
				design: {
					"treatment*": {
						"a": { },
						"b": {
							"*": 2
						},
						"c": {
							"*": 3
						}
					}
				}
			};
			const table = flattenDesign(design);
			//printRows(table);
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
				design: {
					"treatment*": {
						"A": { "*": 2 },
						"B": { "*": 2 },
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
			//printRows(table);
			should.deepEqual(table, [
				{treatment: "A", order: 1},
				{treatment: "A", order: 2},
				{treatment: "B", order: 3},
				{treatment: "B", order: 4},
			]);
		});

		it("should handle a branching array of objects", () => {
			const design = {
				design: {
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
			//printRows(table);
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
					"foot=calculate": "(left == material) ? \"left\" : \"right\""
				},
				design: {
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
					"foot=calculate": "(left == material) ? \"left\" : \"right\""
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
						design: {
							"material": "B",
							"foot=calculate": '(foot == "L") ? "R" : "L"'
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
			printRows(table);
			should.deepEqual(table, [
				{material: "A", boy: 1, foot: 1},
				{treatment: "A", order: 2},
				{treatment: "B", order: 3},
				{treatment: "B", order: 4},
			]);
		});

		it("should keep factors in the correct order", () => {
			const design =
				{ design:
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
			//printRows(table);
			should.deepEqual(_.keys(table[0]),
				["waterSource", "waterVolume", "proteinSource", "proteinVolume", "bufferSystem", "acidSource", "baseSource", "acidPH", "basePH"]
			);
		});

		it("should support numbers with units in range() assignment", () => {
			const design =
				{ design:
					{ source: "water",
						'volume*=range': { count: 4, from: 0, till: 20, decimals: 1, units: "ul" } } };
			const table = flattenDesign(design);
			//printRows(table);
			should.deepEqual(table, [
				{source: "water", volume: "0 ul"},
				{source: "water", volume: "6.7 ul"},
				{source: "water", volume: "13.3 ul"},
				{source: "water", volume: "20 ul"}
			]);
		});

		it("should support calculate() assignments", () => {
			const design =
				{ design:
					{ volume1: "20ul",
						"volume2=calculate": "30ul - volume1" } };
			const table = flattenDesign(design);
			//printRows(table);
			should.deepEqual(table, [
				{volume1: "20ul", volume2: "10 ul"}
			]);
		});

		it("should support assign() with calculate parameters", () => {
			const design = {
				design: {
					source: 'saltwater',
					acidPH: 3.75,
					basePH: 5.75,
					'acidVolume*=range': { count: 4, from: 0, till: 20, decimals: 1, units: 'ul' },
					'baseVolume=assign': {
						calculate: '20ul - acidVolume',
						decimals: 3,
						units: "ml"
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printRows(table);
			should.deepEqual(table, [
				{"source":"saltwater","acidPH":3.75,"basePH":5.75,"acidVolume":"0 ul","baseVolume":"0.020 ml"},
				{"source":"saltwater","acidPH":3.75,"basePH":5.75,"acidVolume":"6.7 ul","baseVolume":"0.013 ml"},
				{"source":"saltwater","acidPH":3.75,"basePH":5.75,"acidVolume":"13.3 ul","baseVolume":"0.007 ml"},
				{"source":"saltwater","acidPH":3.75,"basePH":5.75,"acidVolume":"20 ul","baseVolume":"0.000 ml"}
			]);
		});

		it("should support allocatePlates() assignments", () => {
			const design1 = {
				design: {
					"condition*": [1, 2, 3],
					"replicate*": [1, 2],
					"plate=allocatePlates": {
						wellsPerPlate: 4,
						plates: ["p1", "p2", "p3"],
					}
				}
			};
			const table1 = flattenDesign(design1);
			// console.log(JSON.stringify(table1))
			// printRows(table1);
			should.deepEqual(table1, [
				{condition:1,replicate:1,plate:"p1"},
				{condition:1,replicate:2,plate:"p1"},
				{condition:2,replicate:1,plate:"p1"},
				{condition:2,replicate:2,plate:"p1"},
				{condition:3,replicate:1,plate:"p2"},
				{condition:3,replicate:2,plate:"p2"},
			]);
		});

		it("should support allocatePlates() assignments with 'groupBy' segmentation", () => {
			const design1 = {
				design: {
					"condition*": [1, 2, 3],
					"replicate*": [1, 2],
					"plate=allocatePlates": {
						groupBy: "condition",
						wellsPerPlate: 5,
						plates: ["p1", "p2", "p3"],
					}
				}
			};
			const table1 = flattenDesign(design1);
			// console.log(JSON.stringify(table1))
			// printRows(table1);
			should.deepEqual(table1, [
				{condition:1,replicate:1,plate:"p1"},
				{condition:1,replicate:2,plate:"p1"},
				{condition:2,replicate:1,plate:"p1"},
				{condition:2,replicate:2,plate:"p1"},
				{condition:3,replicate:1,plate:"p2"},
				{condition:3,replicate:2,plate:"p2"},
			]);
		});

		it("should support allocateWells() assignments", () => {
			const design1 = {
				design: {
					"replicate*=range": {till: 5},
					"well=allocateWells": {rows: 2, columns: 3}
				}
			};
			const table1 = flattenDesign(design1);
			// console.log(JSON.stringify(table1))
			// printRows(table1);
			should.deepEqual(table1, [
				{"replicate":1,"well":"A01"},
				{"replicate":2,"well":"B01"},
				{"replicate":3,"well":"A02"},
				{"replicate":4,"well":"B02"},
				{"replicate":5,"well":"A03"}
			]);

			const design2 = {
				design: {
					"replicate*=range": {till: 5},
					"well=allocateWells": {rows: 2, columns: 3, byColumns: false}
				}
			};
			const table2 = flattenDesign(design2);
			// console.log(JSON.stringify(table1))
			// printRows(table1);
			should.deepEqual(table2, [
				{"replicate":1,"well":"A01"},
				{"replicate":2,"well":"A02"},
				{"replicate":3,"well":"A03"},
				{"replicate":4,"well":"B01"},
				{"replicate":5,"well":"B02"}
			]);
		});

		it("should support allocateWells() assignments on a subset of plate", () => {
			const design1 = {
				design: {
					"replicate*=range": {till: 5},
					"well=allocateWells": {rows: 8, columns: 12, wells: "A01 down block B03"}
				}
			};
			const table1 = flattenDesign(design1);
			// console.log(JSON.stringify(table1))
			// printRows(table1);
			should.deepEqual(table1, [
				{"replicate":1,"well":"A01"},
				{"replicate":2,"well":"B01"},
				{"replicate":3,"well":"A02"},
				{"replicate":4,"well":"B02"},
				{"replicate":5,"well":"A03"}
			]);
		});

		it("should support allocateWells() assignments on multiple plates with and orderBy field", () => {
			const design1 = {
				design: {
					"plate*": ["A", "B"],
					"seq*": [2, 1],
					"*": 2,
					"well=allocateWells": {
						rows: 8, columns: 12, wells: "A01 right A04",
						groupBy: "plate",
						orderBy: "seq"
					}
				}
			};
			const table1 = flattenDesign(design1);
			// console.log(JSON.stringify(table1))
			// printRows(table1);
			should.deepEqual(table1, [
				{"plate":"A","seq":2,"well":"A03"},
				{"plate":"A","seq":2,"well":"A04"},
				{"plate":"A","seq":1,"well":"A01"},
				{"plate":"A","seq":1,"well":"A02"},
				{"plate":"B","seq":2,"well":"A03"},
				{"plate":"B","seq":2,"well":"A04"},
				{"plate":"B","seq":1,"well":"A01"},
				{"plate":"B","seq":1,"well":"A02"}
			]);
		});

		it("should support range() with groupBy", () => {
			const design = {
				design: {
					"a*": [1, 2],
					"b*": [1, 2, 3],
					"order=range": {
						groupBy: "a"
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printRows(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":2},{"a":1,"b":3,"order":3},
				{"a":2,"b":1,"order":1},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":3}
			]);
		});

		it("should support range() with sameBy", () => {
			const design = {
				design: {
					"a*": [1, 2],
					"b*": [1, 2, 3],
					"order=range": {
						sameBy: "a"
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printRows(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":1},{"a":1,"b":3,"order":1},
				{"a":2,"b":1,"order":2},{"a":2,"b":2,"order":2},{"a":2,"b":3,"order":2}
			]);
		});

		it("should support range() with groupBy and shuffle", () => {
			const design = {
				randomSeed: 1,
				design: {
					"a*": [1, 2],
					"b*": [1, 2, 3],
					"order=range": {
						groupBy: "a",
						order: "shuffle"
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printRows(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"order":1},{"a":1,"b":2,"order":3},{"a":1,"b":3,"order":2},
				{"a":2,"b":1,"order":2},{"a":2,"b":2,"order":3},{"a":2,"b":3,"order":1}
			]);
		});

		it("should support range() with groupBy and shuffle and shuffleOnce", () => {
			const design = {
				randomSeed: 444,
				design: {
					"a*": [1, 2],
					"b*": [1, 2, 3],
					"order=range": {
						till: 3,
						groupBy: "a",
						order: "shuffle",
						shuffleOnce: true
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printRows(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"order":2},{"a":1,"b":2,"order":1},{"a":1,"b":3,"order":3},
				{"a":2,"b":1,"order":2},{"a":2,"b":2,"order":1},{"a":2,"b":3,"order":3}
			]);
		});

		it("should support assignment of an array of objects", () => {
			const design = {
				design: {
					"a*": [1, 2],
					"b": [{c: 1}, {c: 2}]
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printRows(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"c":1}, {"a":2,"b":2,"c":2}
			]);
		});

		it("should support assignment of a nested array of objects, implicitly resulting in branching", () => {
			const design = {
				design: {
					"a*": [1, 2],
					"b": [[{c: 1}, {c: 2}], [{c: 3}, {c: 4}]]
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printRows(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"c":1}, {"a":1,"b":1,"c":2},
				{"a":2,"b":2,"c":3}, {"a":2,"b":2,"c":4},
			]);
		});

		it("should handle previous bug #1", () => {
			const design = {
				design: {
					"x*": {
						A: {
							"b*": [1, 2],
							"c=": {
								values: ["C", "D"]
							}
						},
						B: {
							"c*": {
								E: {}
							}
						}
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printRows(table);
			should.deepEqual(table, [
				{x: "A", b: 1, c: "C"},
				{x: "A", b: 2, c: "D"},
				{x: "B", c: "E"},
			]);
		});

		it("should handle previous bug #2", () => {
			const design = {
				design: {
					n: 2,
					"i*=range": {groupBy: "n", till: "n"},
					"j=calculate": "i"
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printRows(table);
			should.deepEqual(table, [
				{n: 2, i: 1, j: 1},
				{n: 2, i: 2, j: 2},
			]);
		});

		it("should handle previous bug #3", () => {
			const design = {
				design: {
					"a*": 2,
					"b*": 2,
					"c=": {
						values: [1, 2],
						order: "repeat",
						sameBy: ["a", "b"]
					}
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table))
			// printRows(table);
			should.deepEqual(table, [
				{a: 1, b: 1, c: 1},
				{a: 1, b: 2, c: 2},
				{a: 2, b: 1, c: 1},
				{a: 2, b: 2, c: 2},
			]);
		});

		it.skip("should support sampling assignment of a nested array of objects, implicitly resulting in branching", () => {
			const design = {
				design: {
					"a*": [1, 2, 3, 4],
					"b=sample": [[{c: 1}, {c: 2}], [{c: 3}, {c: 4}]]
				}
			};
			const table = flattenDesign(design);
			console.log(JSON.stringify(table))
			printRows(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"c":1}, {"a":1,"b":1,"c":2},
				{"a":2,"b":2,"c":3}, {"a":2,"b":2,"c":4},
				{"a":3,"b":1,"c":1}, {"a":3,"b":1,"c":2},
				{"a":4,"b":2,"c":3}, {"a":4,"b":2,"c":4},
			]);
		});

		it("should handle 'select' property", () => {
			const design = {
				design: {
					"a*": 2,
					"b*": 2,
					"c*": 2
				},
				select: ["c", "b"]
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table)); printRows(table);
			should.deepEqual(table, [
				{c: 1, b: 1}, {c: 2, b: 1},
				{c: 1, b: 2}, {c: 2, b: 2},
				{c: 1, b: 1}, {c: 2, b: 1},
				{c: 1, b: 2}, {c: 2, b: 2}
			]);
		});

		it("should handle 'orderBy' property", () => {
			const design = {
				design: {
					"a*": 2,
					"b*": 2,
					"c*": 2
				},
				orderBy: "c"
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table)); printRows(table);
			should.deepEqual(table, [
				{"a":1,"b":1,"c":1},
				{"a":1,"b":2,"c":1},
				{"a":2,"b":1,"c":1},
				{"a":2,"b":2,"c":1},
				{"a":1,"b":1,"c":2},
				{"a":1,"b":2,"c":2},
				{"a":2,"b":1,"c":2},
				{"a":2,"b":2,"c":2},
			]);
		});

		it("should handle 'where' property", () => {
			const design = {
				design: {
					"a*": 3
				},
				where: {a: {lte: 2}}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table)); printRows(table);
			should.deepEqual(table, [
				{a: 1}, {a: 2}
			]);
		});

		it("should handle 'initialRows' property", () => {
			const design = {
				initialRows: [
					{a: 1}, {a: 2}, {a: 3}
				],
				design: {
					"b": 1
				}
			};
			const table = flattenDesign(design);
			// console.log(JSON.stringify(table)); printRows(table);
			should.deepEqual(table, [
				{a: 1, b: 1}, {a: 2, b: 1}, {a: 3, b: 1}
			]);
		});

	});
});
