var _ = require('lodash');
var assert = require('assert');
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
			should.deepEqual(result.output.steps[1][1].duration, "3 s");
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

		it("it should handle ?-properties in objects and steps", () => {
			var protocol = {
				roboliq: "v1",
				objects: {
					here: {
						type: 'Site',
					},
					plate1: {
						type: 'Plate',
						'model?': {
							description: "please provide a model"
						},
						'location?': {
							description: "please provide a location",
							'value!': "here"
						}
					},
				},
				steps: {
					1: {
						command: "timer.sleep",
						'duration?': {
							description: "please provide a duration"
						}
					}
				}
			};
			var result = roboliq.run(["-o", "", "--quiet"], protocol);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'));
			assert(_.size(result.output.errors) > 0, "should have an error due to missing steps.1.duration");
			should.deepEqual(result.output.fillIns, {
				'objects.plate1.model': {description: "please provide a model"},
				'steps.1.duration': {description: "please provide a duration"},
			});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.objects.plate1.location, "here");
		});

		it("it should handle !-properties in objects and steps", () => {
			var protocol = {
				roboliq: "v1",
				objects: {
					here: {
						type: 'Site',
					},
					there: {
						type: 'Site',
					},
					'plate1!': {
						type: 'Plate',
						location: "here"
					},
					'plate2': {
						type: 'Plate',
						'location!': "there"
					},
				},
				steps: {
					1: {
						command: "timer.sleep",
						'duration!': 3
					}
				}
			};
			var result = roboliq.run(["-o", ""], protocol);
			//console.log("result:\n"+JSON.stringify(result, null, '\t'));
			should.deepEqual(result.output.objects.plate1.location, "here");
			should.deepEqual(result.output.objects.plate2.location, "there");
			should.deepEqual(result.output.steps['1'].duration, 3);
		});
	});
});
