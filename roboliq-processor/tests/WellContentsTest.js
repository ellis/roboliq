/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

import _ from 'lodash';
import assert from 'assert';
import * as WellContents from '../src/WellContents.js';

describe('WellContents', function() {
	describe('getContentsAndName', function () {

		it('assert find well contents', function () {
			var contents1 = ["1ul", "reagent1"];
			var contents2 = ["2ul", "reagent2"];
			var contents3 = ["3ul", "reagent3"];
			var data = {
				objects: {
					plateModel1: {
						rows: 8,
						columen: 12
					},
					plate1: {
						type: "Plate",
						model: "plateModel1",
						contents: contents1
					},
					plate2: {
						type: "Plate",
						model: "plateModel1",
						contents: {
							A01: contents2
						}
					}
				}
			};
			assert.deepEqual(WellContents.getContentsAndName("plate1", data), [contents1, 'plate1.contents']);
			assert.deepEqual(WellContents.getContentsAndName("plate1(A01)", data), [contents1, 'plate1.contents']);
			assert.deepEqual(WellContents.getContentsAndName("plate2(A01)", data), [contents2, 'plate2.contents.A01']);
		});
	});

	describe("flattenContents", function() {
		it("assert flatten []", function() {
			assert.deepEqual(WellContents.flattenContents([]), {});
		});
		it("assert flatten [0l]", function() {
			assert.deepEqual(WellContents.flattenContents(["0l"]), {});
		});
		it("assert flatten [10ul, reagent1]", function() {
			assert.deepEqual(WellContents.flattenContents(['10ul', 'reagent1']), {reagent1: '10 ul'});
		});
		it("assert flatten [30ul, [10ul, reagent1], [20ul, reagent2]]", function() {
			var contents = ['30ul', ['10ul', 'reagent1'], ['20ul', 'reagent2']];
			assert.deepEqual(WellContents.flattenContents(contents), {
				reagent1: '10 ul',
				reagent2: '20 ul'
			});
		});
		it("assert flatten [30ul, [20ul, reagent1], [40ul, reagent1]]", function() {
			var contents = ['30ul', ['20ul', 'reagent1'], ['40ul', 'reagent1']];
			assert.deepEqual(WellContents.flattenContents(contents), {
				reagent1: '30 ul'
			});
		});
		it("assert flatten [30ul, [10ul, reagent1], [20ul, [10ul, reagent1], [10ul, reagent2], [20ul, water]]]", function() {
			var contents = ['30ul', ['10ul', 'reagent1'], ['20ul', ['10ul', 'reagent1'], ['10ul', 'reagent2'], ['20ul', 'water']]];
			assert.deepEqual(WellContents.flattenContents(contents), {
				reagent1: '15 ul',
				reagent2: '5 ul',
				water: '10 ul'
			});
		});
		it(`assert flatten ["2.5 ul",["2.5 ul","systemLiquid"],["2.5 ul","systemLiquid"],["2.5 ul",["20 ul","strain1"],["80 ul","media1"]]]`, function() {
			var contents = ["2.5 ul",["2.5 ul","systemLiquid"],["2.5 ul","systemLiquid"],["2.5 ul",["20 ul","strain1"],["80 ul","media1"]]];
			assert.deepEqual(WellContents.flattenContents(contents), {
				"media1": "0.6667 ul",
				"strain1": "0.1667 ul",
				"systemLiquid": "1.667 ul"
			});
		});
	});

	describe("mergeContents", () => {
		it("works", () => {
			const contents = ['30ul', ['10ul', 'reagent1'], ['20ul', ['10ul', 'reagent1'], ['10ul', 'reagent2'], ['20ul', 'water']]];
			assert.deepEqual(
				WellContents.mergeContents(contents),
				["30 ul", ["15 ul", "reagent1"], ["5 ul", "reagent2"], ["10 ul", "water"]]
			);
		});
	});

	describe('transferContents', function () {
		it('assert transfer contents between wells', function () {
			assert.deepEqual(
				WellContents.transferContents(['100 ul', 'source1'], undefined, '20 ul'),
				[['80 ul', 'source1'], ['20 ul', 'source1']]
			);
			assert.deepEqual(
				WellContents.transferContents(['Infinity l', 'source1'], undefined, '20 ul'),
				[['Infinity l', 'source1'], ['20 ul', 'source1']]
			);
			assert.deepEqual(
				WellContents.transferContents(['100 ul', 'source1'], ['0 ul'], '20 ul'),
				[['80 ul', 'source1'], ['20 ul', 'source1']]
			);
			assert.deepEqual(
				WellContents.transferContents(['100 ul', 'source1'], ['10 ul', 'source2'], '20 ul'),
				[['80 ul', 'source1'], ['30 ul', ['10 ul', 'source2'], ['20 ul', 'source1']]]
			);
			assert.deepEqual(
				WellContents.transferContents(['100 ul', ['50 ul', 'a'], ['50 ul', 'b']], undefined, '20 ul'),
				[['80 ul', ['50 ul', 'a'], ['50 ul', 'b']], ['20 ul', ['50 ul', 'a'], ['50 ul', 'b']]]
			);
			assert.deepEqual(
				WellContents.transferContents(['100 ul', ['50 ul', 'a'], ['50 ul', 'b']], ['10 ul', 'c'], '20 ul'),
				[['80 ul', ['50 ul', 'a'], ['50 ul', 'b']], ['30 ul', ['10 ul', 'c'], ['20 ul', ['50 ul', 'a'], ['50 ul', 'b']]]]
			);
			assert.deepEqual(
				WellContents.transferContents(["Infinity l", "hepes_850"], ["40 ul", "saltwater"], "30ul"),
				[["Infinity l", "hepes_850"], ["70 ul", ["40 ul", "saltwater"], ["30 ul", "hepes_850"]]]
			);
			assert.deepEqual(
				WellContents.transferContents(["Infinity l", "hepes_650"], ["62.5 ul", ["40 ul", "saltwater"], ["22.5 ul", "hepes_850"]], "7.5ul"),
				[["Infinity l", "hepes_650"], ["70 ul", ["40 ul", "saltwater"], ["22.5 ul", "hepes_850"], ["7.5 ul", "hepes_650"]]]
			);
			assert.deepEqual(
				WellContents.transferContents(["Infinity l", "c"], ["50 ul", ["50 ul", "a"], ["50 ul", "b"]], "10ul"),
				[["Infinity l", "c"], ["60 ul", ["50 ul", ["50 ul", "a"], ["50 ul", "b"]], ["10 ul", "c"]]]
			);
		});
	});

});
