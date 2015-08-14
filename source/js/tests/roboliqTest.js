var _ = require('lodash');
var should = require('should');
var roboliq = require('../roboliq.js')

describe('roboliq', function() {
	describe('roboliq.run', function () {

		it('should handle directives in objects correctly', function () {
			var protocol = {
				objects: {
					plateModel1: {
						type: "PlateModel",
						rows: 8,
						columns: 12
					},
					plate1: {
						type: "Plate",
						model: "plateModel1",
						location: "ourlab.mario.site.P2"
					},
					plate2: {
						type: "Plate",
						model: "plateModel1",
						location: "ourlab.mario.site.P3"
					},
					mixtures: {
						type: 'Variable',
						value: [
							[{source: 'a'}, {source: 'b'}],
							[{source: 'c'}, {source: 'd'}],
						]
					},
					mixtureWells: {
						type: 'Variable',
						value: {"#take": {
							list: "#destinationWells#plate1(all)",
							count: "#length#mixtures"
						}}
					},
					balanceWells: {
						type: 'Variable',
						value: {"#replaceLabware": {
							list: 'mixtureWells',
							new: 'plate2'
						}}
					}
				}
			};
			var result = roboliq.run(["-o", ""], protocol);
			should.deepEqual(result.output.objects.mixtureWells.value,
				['plate1(A01)', 'plate1(B01)']
			);
			should.deepEqual(result.output.objects.balanceWells.value,
				['plate2(A01)', 'plate2(B01)']
			);
		});

		it("should handle imports", function() {
			var protocol1 = {
				objects: {
					plateModel1: {
						type: "PlateModel",
						rows: 8,
						columns: 12
					},
					plate1: {
						type: "Plate",
						location: "ourlab.mario.site.P2"
					}
				}
			};
			var protocol2 = {
				objects: {
					plate1: {
						model: "plateModel1"
					}
				}
			};
			var result = roboliq.run(["-o", "", "--file-json", "./protocol1.json:"+JSON.stringify(protocol1)], protocol2);
			should.deepEqual(result.output.objects.plate1, {
				type: "Plate",
				location: "ourlab.mario.site.P2",
				model: "plateModel1"
			});
		});
	});
});
