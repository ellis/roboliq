var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('roboliq', function() {
	describe('roboliq.run', function () {

		it('should handle directives in objects correctly', function () {
			var protocol = {
				roboliq: "v1",
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
						calculate: {"#take": {
							list: "#destinationWells#plate1(all)",
							count: "#length#mixtures"
						}}
					},
					balanceWells: {
						type: 'Variable',
						calculate: {"#replaceLabware": {
							list: 'mixtureWells',
							new: 'plate2'
						}}
					}
				}
			};
			var result = roboliq.run(["-o", ""], protocol);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'));
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.objects.mixtureWells.value,
				['plate1(A01)', 'plate1(B01)']
			);
			should.deepEqual(result.output.objects.balanceWells.value,
				['plate2(A01)', 'plate2(B01)']
			);
		});

		it("should handle directives in steps (#1)", () => {
			var protocol = {
				roboliq: "v1",
				objects: {
					list1: {
						type: 'Variable',
						value: [1,2,3]
					},
				},
				steps: {
					1: {
						command: "timer.sleep",
						duration: "#length#list1"
					}
				}
			};
			var result = roboliq.run(["-o", ""], protocol);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'));
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.steps[1].duration, "#length#list1");
			should.deepEqual(result.output.steps[1][1].duration, 3);
		});

		it("should handle imports", function() {
			var protocol1 = {
				roboliq: "v1",
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
				roboliq: "v1",
				imports: ["./protocol1.json"],
				objects: {
					plate1: {
						model: "plateModel1"
					}
				}
			};
			var result = roboliq.run(["-o", "", "--file-json", "./protocol1.json:"+JSON.stringify(protocol1)], protocol2);
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.objects.plate1, {
				type: "Plate",
				location: "ourlab.mario.site.P2",
				model: "plateModel1"
			});
		});
	});
});
