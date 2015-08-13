var roboliq = require('../roboliq.js')
var should = require('should');
var pipetterUtils = require('../commands/pipetter/pipetterUtils.js');

describe('pipetter/pipetterUtils', function() {
	describe('getContentsAndName', function () {

		it('should find well contents', function () {
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
			should.deepEqual(pipetterUtils.getContentsAndName("plate1", data), [contents1, 'plate1.contents']);
			should.deepEqual(pipetterUtils.getContentsAndName("plate1(A01)", data), [contents1, 'plate1.contents']);
			should.deepEqual(pipetterUtils.getContentsAndName("plate2(A01)", data), [contents2, 'plate2.contents.A01']);
		});
	});

	describe("flattenContents", function() {
		it("should flatten []", function() {
			should.deepEqual(pipetterUtils.flattenContents([]), {});
		});
		it("should flatten [0l]", function() {
			should.deepEqual(pipetterUtils.flattenContents(["0l"]), {});
		});
		it("should flatten [10ul, reagent1]", function() {
			should.deepEqual(pipetterUtils.flattenContents(['10ul', 'reagent1']), {reagent1: '10 ul'});
		});
		it("should flatten [30ul, [10ul, reagent1], [20ul, reagent2]]", function() {
			var contents = ['30ul', ['10ul', 'reagent1'], ['20ul', 'reagent2']];
			should.deepEqual(pipetterUtils.flattenContents(contents), {
				reagent1: '10 ul',
				reagent2: '20 ul'
			});
		});
		it("should flatten [30ul, [20ul, reagent1], [40ul, reagent1]]", function() {
			var contents = ['30ul', ['20ul', 'reagent1'], ['40ul', 'reagent1']];
			should.deepEqual(pipetterUtils.flattenContents(contents), {
				reagent1: '30 ul'
			});
		});
		it("should flatten [30ul, [10ul, reagent1], [20ul, [10ul, reagent1], [10ul, reagent2], [20ul, water]]]", function() {
			var contents = ['30ul', ['10ul', 'reagent1'], ['20ul', ['10ul', 'reagent1'], ['10ul', 'reagent2'], ['20ul', 'water']]];
			should.deepEqual(pipetterUtils.flattenContents(contents), {
				reagent1: '15 ul',
				reagent2: '5 ul',
				water: '10 ul'
			});
		});
	});
});
