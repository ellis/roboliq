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
		})
	})
})
