var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('pipetter', function() {
	describe('pipetter._dispense', function () {
		it("should update the syringe and destination state", function() {
			var protocol = {
				roboliq: "v1",
				objects: {
					plate1: {
						type: "Plate",
						model: "ourlab.model.plateModel_96_square_transparent_nunc",
						location: "ourlab.mario.site.P2"
					},
					liquid1: {
						type: "Variable",
						value: "ourlab.mario.systemLiquid"
					},
					ourlab: {
						mario: {
							liha: {
								syringe: {
									1: {
										contaminants: ["water"],
										contents: ["20 ul", "water"]
									}
								}
							}
						}
					}
				},
				steps: {
					"1": {
						command: "pipetter._dispense",
						agent: "ourlab.mario.evoware",
						equipment: "ourlab.mario.liha",
						program: "\"Roboliq_Water_Dry_1000\"",
						items: [
							{
								syringe: "ourlab.mario.liha.syringe.1",
								destination: "plate1(A01)",
								volume: "10 ul"
							}
						]
					}
				}
			};
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log(JSON.stringify(result.output.effects, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.effects, {
				"1": {
					"ourlab.mario.liha.syringe.1.contents": [
						"10 ul",
						"water"
					],
					"plate1.contents.A01": [
						"10 ul",
						"water"
					],
					"__WELLS__.plate1.contents.A01": {
						"isSource": false,
						"volumeMin": "0 l",
						"volumeMax": "10 ul",
						"volumeAdded": "10 ul"
					}
				}
			});
		});
	});
});
